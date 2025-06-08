package org.myhab.config;
import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestException;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.JSONConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.builder.BasicConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.io.FileHandler;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

class ConfigProvider implements InitializingBean {
    private Date lastSyncDate = Date.from(Instant.ofEpochSecond(0));
    private final static CompositeConfiguration config = new CompositeConfiguration();
    private File cloneDir;
    private Git git;
    String repoURI;
    String branch;
    String username;
    String password;

    public ConfigProvider() {
        try {
            cloneDir = Files.createTempDirectory("config").toFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    <T> T get(final Class<T> cls, final String key) {
        return config.get(cls, key);
    }

    <T> List<T> getList(final Class<T> cls, final String key) {
        return config.getList(cls, key);
    }

    void asyncLoad(Function fn) {
        CompletableFuture.supplyAsync(this::syncLoad).thenApply(fn);
    }

    private boolean syncLoad() {
        boolean result = false;
        if (ping(repoURI)) {
            try {
                git.pull().setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).call();
                Repository repository = git.getRepository();
                ObjectId lastCommitId = repository.resolve(Constants.HEAD);
                RevWalk revWalk = new RevWalk(repository);
                RevCommit commit = revWalk.parseCommit(lastCommitId);
                if (Date.from(Instant.ofEpochSecond(commit.getCommitTime())).after(lastSyncDate)) {
                    RevTree tree = commit.getTree();
                    TreeWalk treeWalk = new TreeWalk(repository);
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
                    config.clear();
                    while (treeWalk.next()) {
                        ObjectId objectId = treeWalk.getObjectId(0);
                        ObjectLoader loader = repository.open(objectId);

                        if (treeWalk.getPathString().endsWith("yaml") || treeWalk.getPathString().endsWith("yml")) {
                            YAMLConfiguration yamlConf = new YAMLConfiguration();
                            yamlConf.read(new StringReader(new String(loader.getBytes())));
                            config.addConfiguration(yamlConf);
                        } else if (treeWalk.getPathString().endsWith("xml")) {
                            XMLConfiguration cfg = new BasicConfigurationBuilder<>(XMLConfiguration.class).configure(new Parameters().xml()).getConfiguration();
                            FileHandler fh = new FileHandler(cfg);
                            fh.load(new StringReader(new String(loader.getBytes())));
                            config.addConfiguration(cfg);
                        } else if (treeWalk.getPathString().endsWith("json")) {
                            JSONConfiguration jsonConf = new JSONConfiguration();
                            jsonConf.read(new StringReader(new String(loader.getBytes())));
                            config.addConfiguration(jsonConf);
                        }
                    }
                    lastSyncDate = new Date();
                    result = true;
                    System.out.println("Config synchronization");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return result;
    }

    boolean ping(String uri) {
        try {
            return Unirest.get(uri).asString().getStatus() < 400;
        } catch (UnirestException ex) {
            return false;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Unirest.config()
                .reset()
                .connectTimeout(2000)
                .setDefaultHeader("Accept", "application/json")
                .followRedirects(true)
                .enableCookieManagement(false);


        git = Git.cloneRepository()
                .setURI(getRepoURI())
                .setDirectory(cloneDir)
                .setBranchesToClone(Arrays.asList("refs/heads/" + branch))// give ur branch name
                .setBranch("refs/heads/" + branch)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                .call();
        if (syncLoad()) {
            config.getKeys().forEachRemaining(key -> {
                System.out.println(key + " = " + config.getProperty(key));
            });
        } else {
            System.err.println("Error read config");
        }
    }

    public Git getGit() {
        return git;
    }

    public void setGit(Git git) {
        this.git = git;
    }

    public String getRepoURI() {
        return repoURI;
    }

    public void setRepoURI(String repoURI) {
        this.repoURI = repoURI;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
