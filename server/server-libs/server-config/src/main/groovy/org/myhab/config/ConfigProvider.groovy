package org.myhab.config

import groovy.transform.Synchronized
import groovy.util.logging.Slf4j
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

import java.time.Instant
import java.util.concurrent.CompletableFuture

@Slf4j
class ConfigProvider implements InitializingBean {
    private Date lastSyncDate = Date.from(Instant.ofEpochSecond(0))
    private final static CompositeConfiguration config = new CompositeConfiguration()
    private final static File cloneDir = File.createTempDir()
    private Git git
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
        boolean result
        if (ping(repoURI)) {
            try {
                git.pull().setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).call()
                def repository = git.getRepository()
                ObjectId lastCommitId = repository.resolve(Constants.HEAD);
                RevWalk revWalk = new RevWalk(repository)
                RevCommit commit = revWalk.parseCommit(lastCommitId);
                if (Date.from(Instant.ofEpochSecond(commit.getCommitTime())).after(lastSyncDate)) {
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
                    lastSyncDate = new Date()
                    result = true
                    log.debug("Config synchronization")
                }

            } catch (Exception ex) {
                ex.printStackTrace()
                log.error(ex.message)
            }
        }

        return result
    }

    boolean ping(uri) {
        try {
            return Unirest.get(uri).asString().status < 400
        } catch (UnirestException ex) {
            return false
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


        git = Git.cloneRepository()
                .setURI("${repoURI}")
                .setDirectory(cloneDir)
                .setBranchesToClone(Arrays.asList("refs/heads/" + branch))// give ur branch name
                .setBranch("refs/heads/" + branch)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                .call();
        if (syncLoad()) {
            config.keys.each { key ->
                println "$key = ${config.get(Object.class, key)}"
            }
        } else {
            println("Error read config")
        }
    }
}
