package io.crdb.docker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Boot application for performing remote CockroachDB administrative tasks.
 * This application is designed to run as a one-time task container that connects to
 * a CockroachDB cluster, performs initialization tasks, and then exits.
 * 
 * <p>The application's behavior is entirely controlled through environment variables,
 * allowing for flexible configuration without code changes. It can perform cluster
 * initialization, database creation, user management, and enterprise license setup.
 * 
 * <p>All CockroachDB operations are executed via ProcessBuilder calling the
 * {@code /cockroach} binary that is included in the Docker image.
 * 
 * @author Generated with Claude Code
 * @version 1.0
 * @since 1.0
 */
@SpringBootApplication
public class RemoteClientApplication implements ApplicationRunner {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(RemoteClientApplication.class);

    /**
     * Environment variable name for CockroachDB host and port (format: host:port).
     */
    private static final String COCKROACH_HOST = "COCKROACH_HOST";
    
    /**
     * Environment variable name for CockroachDB port (if not specified in COCKROACH_HOST).
     */
    private static final String COCKROACH_PORT = "COCKROACH_PORT";
    
    /**
     * Environment variable name for CockroachDB user for the session.
     */
    private static final String COCKROACH_USER = "COCKROACH_USER";
    
    /**
     * Environment variable name for insecure connection flag (true/false).
     */
    private static final String COCKROACH_INSECURE = "COCKROACH_INSECURE";
    
    /**
     * Environment variable name for path to certificate directory.
     */
    private static final String COCKROACH_CERTS_DIR = "COCKROACH_CERTS_DIR";

    /**
     * Environment variable name for database to create.
     */
    private static final String DATABASE_NAME = "DATABASE_NAME";
    
    /**
     * Environment variable name for database user to create (will be granted admin privileges).
     */
    private static final String DATABASE_USER = "DATABASE_USER";
    
    /**
     * Environment variable name for password for DATABASE_USER.
     */
    private static final String DATABASE_PASSWORD = "DATABASE_PASSWORD";
    
    /**
     * Environment variable name for cluster organization setting.
     */
    private static final String COCKROACH_ORG = "COCKROACH_ORG";
    
    /**
     * Environment variable name for enterprise license key.
     */
    private static final String COCKROACH_LICENSE_KEY = "COCKROACH_LICENSE_KEY";
    
    /**
     * Environment variable name for cluster initialization flag.
     */
    private static final String COCKROACH_INIT = "COCKROACH_INIT";

    /**
     * Main entry point for the application.
     * 
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(RemoteClientApplication.class, args);
    }

    /**
     * Spring Environment for accessing configuration properties and environment variables.
     */
    @Autowired
    private Environment env;

    /**
     * Main application logic that executes when the Spring Boot application starts.
     * 
     * <p>This method performs the following operations in sequence:
     * <ol>
     *   <li>Reads and logs all environment variables</li>
     *   <li>Initializes cluster if COCKROACH_INIT is true</li>
     *   <li>Enables remote debugging mode</li>
     *   <li>Sets enterprise license if organization and license key are provided</li>
     *   <li>Creates database if DATABASE_NAME is specified</li>
     *   <li>Creates user and grants privileges if DATABASE_USER is specified</li>
     * </ol>
     * 
     * @param args application arguments (not used in this implementation)
     * @throws Exception if any CockroachDB command fails or I/O errors occur
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {

        // these values/parameters correspond to supported CockroachDB Environment variables
        final String host = env.getProperty(COCKROACH_HOST);
        final Integer port = env.getProperty(COCKROACH_PORT, Integer.class);
        final String user = env.getProperty(COCKROACH_USER);
        final Boolean insecure = env.getProperty(COCKROACH_INSECURE, Boolean.class);
        final String certsDir = env.getProperty(COCKROACH_CERTS_DIR);

        // these values/parameters do not have corresponding CockroachDB Environment variables
        final String databaseName = env.getProperty(DATABASE_NAME);
        final String databaseUser = env.getProperty(DATABASE_USER);
        final String databasePassword = env.getProperty(DATABASE_PASSWORD);
        final String licenseOrg = env.getProperty(COCKROACH_ORG);
        final String licenseKey = env.getProperty(COCKROACH_LICENSE_KEY);
        final boolean initCluster = env.getProperty(COCKROACH_INIT, Boolean.class, Boolean.FALSE);

        log.info("{} is [{}]", COCKROACH_HOST, host);
        log.info("{} is [{}]", COCKROACH_PORT, port);
        log.info("{} is [{}]", COCKROACH_USER, user);
        log.info("{} is [{}]", COCKROACH_INSECURE, insecure);
        log.info("{} is [{}]", COCKROACH_CERTS_DIR, certsDir);

        log.info("{} is [{}]", DATABASE_NAME, databaseName);
        log.info("{} is [{}]", DATABASE_USER, databaseUser);
        log.info("{} is [{}]", DATABASE_PASSWORD, databasePassword);

        log.info("{} is [{}]", COCKROACH_ORG, licenseOrg);
        log.info("{} is [{}]", COCKROACH_LICENSE_KEY, licenseKey);
        log.info("{} is [{}]", COCKROACH_INIT, initCluster);

        if (initCluster) {
            List<String> commands = new ArrayList<>();
            commands.add("/cockroach");
            commands.add("init");
            commands.add("--disable-cluster-name-verification");

            ProcessBuilder builder = new ProcessBuilder(commands);
            handleProcess(builder);
        }

        handleProcess(new ProcessBuilder("/cockroach", "sql", "--execute", "SET CLUSTER SETTING server.remote_debugging.mode = 'any'"));

        if (StringUtils.hasText(licenseOrg) && StringUtils.hasText(licenseKey)) {
            List<String> commands = new ArrayList<>();
            commands.add("/cockroach");
            commands.add("sql");
            commands.add("--execute");
            commands.add(String.format("SET CLUSTER SETTING cluster.organization = '%s'", licenseOrg));
            commands.add("--execute");
            commands.add(String.format("SET CLUSTER SETTING enterprise.license = '%s'", licenseKey));

            ProcessBuilder builder = new ProcessBuilder(commands);
            handleProcess(builder);
        }

        if (StringUtils.hasText(databaseName)) {
            List<String> commands = new ArrayList<>();
            commands.add("/cockroach");
            commands.add("sql");
            commands.add("--execute");
            commands.add(String.format("CREATE DATABASE IF NOT EXISTS %s", databaseName));

            ProcessBuilder builder = new ProcessBuilder(commands);
            handleProcess(builder);
        }

        if (StringUtils.hasText(databaseName) && StringUtils.hasText(databaseUser)) {
            List<String> commands = new ArrayList<>();
            commands.add("/cockroach");
            commands.add("sql");
            commands.add("--execute");

            if(StringUtils.hasText(databasePassword)) {
                commands.add(String.format("CREATE USER IF NOT EXISTS %s WITH PASSWORD '%s'", databaseUser, databasePassword));
            } else {
                commands.add(String.format("CREATE USER IF NOT EXISTS %s WITH PASSWORD NULL", databaseUser));
            }

            commands.add("--execute");
            commands.add(String.format("GRANT ALL ON DATABASE %s TO %s", databaseName, databaseUser));
            commands.add("--execute");
            commands.add(String.format("GRANT admin TO %s", databaseUser));

            ProcessBuilder builder = new ProcessBuilder(commands);
            handleProcess(builder);
        }

    }

    /**
     * Executes a CockroachDB command using ProcessBuilder and handles the result.
     * 
     * <p>This method configures the process to inherit I/O streams, executes the command,
     * and waits for completion. If the process exits with a non-zero code, a RuntimeException
     * is thrown.
     * 
     * @param builder the ProcessBuilder configured with the command to execute
     * @throws IOException if an I/O error occurs during process execution
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @throws RuntimeException if the process exits with a non-zero exit code
     */
    private void handleProcess(ProcessBuilder builder) throws IOException, InterruptedException {

        builder.inheritIO();

        String command = builder.command().toString();

        log.debug("starting command... {}", command);

        Process process = builder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(String.format("the following command exited ABNORMALLY with code [%d]: %s", exitCode, command));
        } else {
            log.debug("command exited SUCCESSFULLY with code [{}]", exitCode);
        }

    }
}
