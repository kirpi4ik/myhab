package org.myhab.config

import org.eclipse.jgit.api.Git
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

/**
 * The configuration a myHAB instance needs to boot at all — the MQTT broker host,
 * port and credentials — lives in a git repo. Loading it used to be gated on an
 * HTTP {@code ping} of the repo URI, so an unreachable repo produced an *empty*
 * configuration rather than a stale one, and {@code MQTTConfiguration} then failed
 * on a null password and took the whole application down with it.
 *
 * These cover the two cases that follow from the fix: a repo that cannot be pinged
 * still yields its committed configuration, and a file:// repo (which has no HTTP
 * endpoint to ping at all) works — which is what the public demo relies on.
 */
class ConfigProviderSpec extends Specification {

    @TempDir
    Path tempDir

    private String bareRepoWith(String branch, String yaml) {
        File work = tempDir.resolve('work').toFile()
        work.mkdirs()
        new File(work, 'config.yaml').text = yaml

        Git git = Git.init().setDirectory(work).setInitialBranch(branch).call()
        git.add().addFilepattern('.').call()
        git.commit().setMessage('seed').setAuthor('t', 't@example.invalid').call()
        git.close()

        File bare = tempDir.resolve('bare.git').toFile()
        Git.cloneRepository()
                .setURI(work.toURI().toString())
                .setDirectory(bare)
                .setBare(true)
                .setBranch(branch)
                .call()
                .close()

        return bare.toURI().toString()
    }

    private ConfigProvider providerFor(String repoUri, String branch) {
        def provider = new ConfigProvider(
                repoURI: repoUri,
                branch: branch,
                // A file:// repo has no credentials; JGit's provider NPEs on a null
                // password, so the bean supplies empty strings.
                username: '',
                password: '')
        provider.afterPropertiesSet()
        return provider
    }

    /**
     * One instance per JVM: ConfigProvider holds its clone directory and its merged
     * configuration in static fields, so a second afterPropertiesSet() would clone
     * into a non-empty directory and fail. That is fine for the application, which
     * has exactly one such bean — but it means this spec exercises a single provider.
     */
    void 'loads configuration from a file:// repository that cannot be pinged'() {
        given: 'a bare repo holding the keys the broker connection needs'
        String uri = bareRepoWith('demo', '''mqtt:
  hostname: demo-mqtt
  port: 1883
  password: demo
  topics: myhab/#
profile: demo
''')

        when:
        def provider = providerFor(uri, 'demo')

        then: 'the values are available even though ping() cannot succeed over file://'
        provider.get(String, 'mqtt.hostname') == 'demo-mqtt'
        provider.get(Integer, 'mqtt.port') == 1883
        // The two that previously NPE'd during context startup when config was empty.
        provider.get(String, 'mqtt.password') == 'demo'
        provider.get(String, 'mqtt.topics') == 'myhab/#'

        and: 'the same values are reachable through the listing the config screen uses'
        def all = provider.getAll()
        all.find { it.key == 'mqtt.hostname' }?.value == 'demo-mqtt'
        all.find { it.key == 'profile' }?.value == 'demo'
    }
}
