package com.example.monitoring.worker.db;

import com.example.monitoring.common.domain.CheckEntity;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.loader.KeyPairResourceLoader;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

@Component
public class CheckSshProvider {

    private static final Logger log = LoggerFactory.getLogger(CheckSshProvider.class);

    private final SshClient client;

    public CheckSshProvider() {
        this.client = SshClient.setUpDefaultClient();
        this.client.start();
    }

    @PreDestroy
    public void shutdown() {
        try {
            client.stop();
        } catch (Exception e) {
            log.warn("Failed to stop ssh client cleanly", e);
        }
    }

    public ExecResult exec(CheckEntity check, String command, long connectTimeoutMs, long commandTimeoutMs) {
        if (!StringUtils.hasText(check.getHost())) {
            throw new IllegalStateException("Check host missing. checkId=" + check.getId());
        }
        int port = (check.getPort() != null) ? check.getPort() : 22;
        if (!StringUtils.hasText(check.getSshUsername())) {
            throw new IllegalStateException("SSH username missing. checkId=" + check.getId());
        }

        String host = check.getHost();

        try (ClientSession session = client.connect(check.getSshUsername(), host, port)
                .verify(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .getSession()) {

            if (StringUtils.hasText(check.getSshPrivateKeyPath())) {
                KeyPair keyPair = loadKeyPair(check.getSshPrivateKeyPath());
                session.addPublicKeyIdentity(keyPair);
            } else if (StringUtils.hasText(check.getSshPassword())) {
                session.addPasswordIdentity(check.getSshPassword());
            } else {
                throw new IllegalStateException("SSH auth missing (password or key). checkId=" + check.getId());
            }

            session.auth().verify(connectTimeoutMs, TimeUnit.MILLISECONDS);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();

            try (ChannelExec channel = session.createExecChannel(command)) {
                channel.setOut(out);
                channel.setErr(err);

                channel.open().verify(connectTimeoutMs, TimeUnit.MILLISECONDS);

                channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), commandTimeoutMs);

                Integer exit = channel.getExitStatus();
                int exitCode = (exit == null) ? -1 : exit;

                String stdout = out.toString(StandardCharsets.UTF_8);
                String stderr = err.toString(StandardCharsets.UTF_8);

                return new ExecResult(exitCode, stdout, stderr);
            }

        } catch (Exception e) {
            throw new IllegalStateException("SSH exec failed. checkId=" + check.getId() + ", host=" + host, e);
        }
    }

    private KeyPair loadKeyPair(String privateKeyPath) throws Exception {
        log.info("BouncyCastle registered={}", SecurityUtils.isBouncyCastleRegistered());

        KeyPairResourceLoader loader = SecurityUtils.getKeyPairResourceParser();
        Iterable<KeyPair> keys = loader.loadKeyPairs(null, Paths.get(privateKeyPath), null);

        var it = keys.iterator();
        if (!it.hasNext()) {
            throw new IllegalStateException("No keypair loaded from: " + privateKeyPath);
        }
        return it.next();
    }

    public record ExecResult(int exitCode, String stdout, String stderr) {}
}
