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
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

import java.time.Instant
import java.util.concurrent.CompletableFuture

@Slf4j
class ConfigProvider implements InitializingBean {
    private Date lastSyncDate = Date.from(Instant.ofEpochSecond(0))
    private final static CompositeConfiguration config = new CompositeConfiguration()
    private final static File cloneDir = File.createTempDir()
    private static final String OVERRIDES_FILE = "overrides.yaml"
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

    /**
     * Get all configuration keys and their values
     * @return List of maps with key and value
     */
    List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = []
        config.getKeys().each { key ->
            def value = config.get(Object.class, key)
            result << [
                key: key,
                value: value?.toString() ?: '',
                type: value?.class?.simpleName ?: 'null'
            ]
        }
        return result.sort { it.key }
    }

    /**
     * Set a configuration value and optionally commit to GIT
     * Values are stored in an overrides.yaml file
     * @param key Configuration key
     * @param value Configuration value
     * @param commitMessage Optional commit message
     * @return true if successful
     */
    @Synchronized
    boolean setAndCommit(String key, String value, String commitMessage = null) {
        try {
            // Load or create overrides file
            File overridesFile = new File(cloneDir, OVERRIDES_FILE)
            Map<String, Object> overrides = [:]
            
            if (overridesFile.exists()) {
                Yaml yaml = new Yaml()
                overrides = yaml.load(overridesFile.text) ?: [:]
            }
            
            // Update the value (support nested keys with dot notation)
            setNestedValue(overrides, key, value)
            
            // Write back to file
            DumperOptions options = new DumperOptions()
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
            options.setPrettyFlow(true)
            Yaml yaml = new Yaml(options)
            overridesFile.text = yaml.dump(overrides)
            
            // Stage the file
            git.add().addFilepattern(OVERRIDES_FILE).call()
            
            // Commit
            String message = commitMessage ?: "Update config: ${key}"
            git.commit()
                .setMessage(message)
                .call()
            
            // Push to remote
            git.push()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                .call()
            
            // Reload configuration
            syncLoad()
            
            log.info("Configuration updated and committed: ${key} = ${value}")
            return true
        } catch (Exception ex) {
            log.error("Failed to update configuration: ${key}", ex)
            return false
        }
    }
    
    /**
     * Set a nested value in a map using dot notation key
     */
    private void setNestedValue(Map<String, Object> map, String key, Object value) {
        String[] parts = key.split("\\.")
        Map<String, Object> current = map
        
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i]
            if (!current.containsKey(part) || !(current[part] instanceof Map)) {
                current[part] = [:]
            }
            current = current[part] as Map<String, Object>
        }
        
        current[parts[parts.length - 1]] = value
    }
    
    /**
     * Refresh configuration from GIT (pull latest changes)
     * @return true if configuration was updated
     */
    boolean refresh() {
        return syncLoad()
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
                    
                    // Collect all configurations: overrides separately, others in a list
                    YAMLConfiguration overridesConf = null
                    List<org.apache.commons.configuration2.Configuration> otherConfigs = []
                    
                    // First pass: load all config files, but handle overrides.yaml separately
                    while (treeWalk.next()) {
                        ObjectId objectId = treeWalk.getObjectId(0)
                        ObjectLoader loader = repository.open(objectId)
                        String filePath = treeWalk.getPathString()

                        if (filePath.endsWith("yaml") || filePath.endsWith("yml")) {
                            YAMLConfiguration yamlConf = new YAMLConfiguration();
                            yamlConf.read(new StringReader(new String(loader.bytes)))
                            
                            // Check if this is the overrides file
                            if (filePath.equals(OVERRIDES_FILE) || filePath.endsWith("/" + OVERRIDES_FILE)) {
                                overridesConf = yamlConf
                                log.debug("Found overrides configuration: ${filePath}")
                            } else {
                                otherConfigs << yamlConf
                            }
                        } else if (filePath.endsWith("xml")) {
                            XMLConfiguration cfg = new BasicConfigurationBuilder<>(XMLConfiguration.class).configure(new Parameters().xml()).getConfiguration();
                            FileHandler fh = new FileHandler(cfg);
                            fh.load(new StringReader(new String(loader.bytes)));
                            otherConfigs << cfg
                        } else if (filePath.endsWith("json")) {
                            JSONConfiguration jsonConf = new JSONConfiguration();
                            jsonConf.read(new StringReader(new String(loader.bytes)))
                            otherConfigs << jsonConf
                        }
                    }
                    
                    // Clear and rebuild config with proper priority
                    // CompositeConfiguration returns value from FIRST config that contains the key
                    config.clear()
                    
                    // Add overrides FIRST so it takes precedence
                    if (overridesConf != null) {
                        config.addConfiguration(overridesConf)
                        log.debug("Added overrides configuration with highest priority")
                    }
                    
                    // Add all other configs after overrides
                    otherConfigs.each { cfg ->
                        config.addConfiguration(cfg)
                    }
                    
                    lastSyncDate = new Date()
                    result = true
                    log.debug("Config synchronization completed with ${otherConfigs.size() + (overridesConf ? 1 : 0)} config files")
                }

            } catch (Exception ex) {
                log.error("Failed to synchronize configuration", ex)
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
                log.debug "$key = ${config.get(Object.class, key)}"
            }
        } else {
            log.error("Error read config")
        }
    }
}
