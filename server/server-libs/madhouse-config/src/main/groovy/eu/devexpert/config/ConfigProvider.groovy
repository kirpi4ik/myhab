package eu.devexpert.config

import groovy.transform.Synchronized
import kong.unirest.Unirest
import kong.unirest.UnirestException
import org.apache.commons.configuration2.CompositeConfiguration
import org.apache.commons.configuration2.JSONConfiguration
import org.apache.commons.configuration2.XMLConfiguration
import org.apache.commons.configuration2.YAMLConfiguration
import org.apache.commons.configuration2.builder.BasicConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.commons.configuration2.io.FileHandler
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectLoader
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.TreeWalk
import org.springframework.beans.factory.InitializingBean

import java.util.concurrent.CompletableFuture

class ConfigProvider implements InitializingBean {
    private final static CompositeConfiguration config = new CompositeConfiguration()
    String repoURI
    String branch
    String username
    String password

    def <T> T get(final Class<T> cls, final String key) {
        return config.get(cls, key)
    }

    def <T> List<T> getList(final Class<T> cls, final String key) {
        return config.getList(cls, key)
    }

    void asyncLoad(fn) {
        CompletableFuture<Boolean> completableFuture = CompletableFuture.supplyAsync({ -> syncLoad() }).thenApply(fn)
    }

    @Synchronized
    private boolean syncLoad() {
        if (ping(repoURI)) {
            try {
                Git git = Git.cloneRepository()
                        .setURI("${repoURI}")
                        .setDirectory(File.createTempDir())
                        .setBranchesToClone(Arrays.asList("refs/heads/" + branch))// give ur branch name
                        .setBranch("refs/heads/" + branch)
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                        .call();
                def repository = git.getRepository()
                ObjectId lastCommitId = repository.resolve(Constants.HEAD);
                RevWalk revWalk = new RevWalk(repository)
                RevCommit commit = revWalk.parseCommit(lastCommitId);
                RevTree tree = commit.getTree();
                TreeWalk treeWalk = new TreeWalk(repository)
                treeWalk.addTree(tree)
                treeWalk.setRecursive(true)
                config.clear()
                while (treeWalk.next()) {
                    ObjectId objectId = treeWalk.getObjectId(0)
                    ObjectLoader loader = repository.open(objectId)

                    if (treeWalk.getPathString().endsWith("yaml") || treeWalk.getPathString().endsWith("yml")) {
                        YAMLConfiguration yamlConf = new YAMLConfiguration();
                        yamlConf.read(new StringReader(new String(loader.bytes)))
                        config.addConfiguration(yamlConf)
                    } else if (treeWalk.getPathString().endsWith("xml")) {
                        XMLConfiguration cfg = new BasicConfigurationBuilder<>(XMLConfiguration.class).configure(new Parameters().xml()).getConfiguration();
                        FileHandler fh = new FileHandler(cfg);
                        fh.load(new StringReader(new String(loader.bytes)));
                        config.addConfiguration(cfg)
                    } else if (treeWalk.getPathString().endsWith("json")) {
                        JSONConfiguration jsonConf = new JSONConfiguration();
                        jsonConf.read(new StringReader(new String(loader.bytes)))
                        config.addConfiguration(jsonConf)
                    }
                }
                return true
            } catch (Exception ex) {
                return false
            }
        } else {
            return false
        }
    }

    boolean ping(uri) {
        try {
            return Unirest.get(uri).asString().status < 400
        } catch (UnirestException ex) {
            return false
        }
    }

    public static void main(String[] args) {

        ConfigProvider cf = new ConfigProvider(
                repoURI: System.getenv("CFG_REPO_URI"),
                username: System.getenv("CFG_USERNAME"),
                password: System.getenv("CFG_PASSWORD"),
                branch: "dev")
        if (cf.asyncLoad()) {
            cf.config.keys.each { key ->
                println "$key=${cf.config.get(Object.class, key)}"
            }
        }
    }

    @Override
    void afterPropertiesSet() throws Exception {
        Unirest.config()
                .reset()
                .socketTimeout(2000)
                .connectTimeout(2000)
                .setDefaultHeader("Accept", "application/json")
                .followRedirects(true)
                .enableCookieManagement(false)
        if (syncLoad()) {
            config.keys.each { key ->
                println "$key = ${config.get(Object.class, key)}"
            }
        } else {
            println("ERRRRRRROR")
        }
    }
}
