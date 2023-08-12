package de.dddns.kirbylink.archiva;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.ResourceUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ActiveProfiles("integrationtest")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ArchivaApplicationIntegrationTest {
	
	@LocalServerPort
    int randomServerPort;
	
	@Autowired
	private TestRestTemplate testRestTemplate;

	private static final String PASSWORD = "abc123";
	private static final String USERNAME = "admin";
	
	private static final String ARTIFACT_NAME = "hello-world";
	private static final String ARTIFACT_VERSION = "0.0.1";
	
	private static final String ARTIFACT_VERSION_SNAPSHOT = ARTIFACT_VERSION + "-SNAPSHOT";
	private static final String ARTIFACT_FILE_NAME = ARTIFACT_NAME + "-" + ARTIFACT_VERSION_SNAPSHOT;
	private static final String ARTIFACT_FILE_NAME_UPLOADED = ARTIFACT_NAME + "-" + ARTIFACT_VERSION;
	
	private static final String URL = "jdbc:derby:memory:users;create=true";
	private static final String BASE_HOST = "http://localhost:";
	private static final String PATH = "/repository/snapshots/com/example/" + ARTIFACT_NAME;
	private static final String DATE_FORMAT_MAVEN_METADATA = "yyyyMMddHHmmss";
	private static final String DATE_FORMAT_MAVEN_METADATA_TIMESTAMP = "yyyyMMdd.HHmmss";
	
	private static final String MAVEN_METADATA_MAIN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<metadata>\n"
			+ "  <groupId>com.example</groupId>\n"
			+ "  <artifactId>%s</artifactId>\n"
			+ "  <versioning>\n"
			+ "    <versions>\n"
			+ "      <version>%s</version>\n"
			+ "    </versions>\n"
			+ "    <lastUpdated>%s</lastUpdated>\n"
			+ "  </versioning>\n"
			+ "</metadata>";
	
	private static final String MAVEN_METADATA_SNAPSHOT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<metadata modelVersion=\"1.1.0\">\n"
			+ "  <groupId>com.example</groupId>\n"
			+ "  <artifactId>%s</artifactId>\n"
			+ "  <version>%s</version>\n"
			+ "  <versioning>\n"
			+ "    <snapshot>\n"
			+ "      <timestamp>%s</timestamp>\n"
			+ "      <buildNumber>1</buildNumber>\n"
			+ "    </snapshot>\n"
			+ "    <lastUpdated>%s</lastUpdated>\n"
			+ "    <snapshotVersions>\n"
			+ "      <snapshotVersion>\n"
			+ "        <extension>jar</extension>\n"
			+ "        <value>%s-%s-1</value>\n"
			+ "        <updated>%s</updated>\n"
			+ "      </snapshotVersion>\n"
			+ "      <snapshotVersion>\n"
			+ "        <extension>pom</extension>\n"
			+ "        <value>%s-%s-1</value>\n"
			+ "        <updated>%s</updated>\n"
			+ "      </snapshotVersion>\n"
			+ "    </snapshotVersions>\n"
			+ "  </versioning>\n"
			+ "</metadata>";
	
	private String dateMavenMetadata;
	private String dateMavenMetadataTimestamp;
	private HttpHeaders headers;
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		Path path = Files.createTempDirectory("archiva");
		System.setProperty("appserver.base", path.toString());
		System.setProperty("appserver.home", path.toString());
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		System.clearProperty("appserver.base");
		System.clearProperty("appserver.home");
	}

	@BeforeEach
	void setUp() throws Exception {
		Date time = Calendar.getInstance().getTime();
		
		DateFormat dateFormatMavenMetadata = new SimpleDateFormat(DATE_FORMAT_MAVEN_METADATA);
		dateMavenMetadata = dateFormatMavenMetadata.format(time);
		
		DateFormat dateFormatMavenMetadataTimestamp = new SimpleDateFormat(DATE_FORMAT_MAVEN_METADATA_TIMESTAMP);
		dateMavenMetadataTimestamp = dateFormatMavenMetadataTimestamp.format(time);
		
		headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		
		log.info("Initialize database");
		initTestDatabase();
		log.info("Initialize database finished");		
	}

	void initTestDatabase() throws Exception {
		ClassLoader classLoader = ArchivaApplicationIntegrationTest.class.getClassLoader();

		Connection connection = DriverManager.getConnection(URL);
		for (Table table : Table.values()) {
			log.info("Restore table '{}'", table);
			File file = new File(classLoader.getResource(table.getValue() + ".txt").getFile());
			String callFormat = "CALL SYSCS_UTIL.SYSCS_IMPORT_TABLE ('SA', '%s', '%s', null, null, null,0)";
			CallableStatement callableStatement = connection.prepareCall(format(callFormat, table.toString(), file.getAbsolutePath()));
			callableStatement.execute();
			callableStatement.close();
		}

		log.info("Restore table 'SECURITY_USERASSIGNMENT_ROLENAMES'");
		Statement statement = connection.createStatement();
		String sql = "INSERT INTO SA.SECURITY_USERASSIGNMENT_ROLENAMES VALUES ('admin','System Administrator',0)";
		statement.execute(sql);
	}

	@Getter
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public enum Table {
		JDOUSER("jdouser"), JDOUSER_PREVIOUSENCODEDPASSWORDS("jdouser_previousencodedpasswords"), SECURITY_USER_ASSIGNMENTS("security_user_assignments");

		private final String value;
	}

	@Test
	void testMavenDeploy() throws Exception {

		String url = BASE_HOST + randomServerPort + PATH;
		
		String path = format("%s/%s/maven-metadata.xml", url, ARTIFACT_VERSION_SNAPSHOT);
		log.info("Downloading from snapshots: {}", path);
		ResponseEntity<String> response = testRestTemplate.withBasicAuth(USERNAME, PASSWORD).getForEntity(path, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		
		path = format("%s/%s/%s-%s-1.jar", url, ARTIFACT_VERSION_SNAPSHOT, ARTIFACT_FILE_NAME_UPLOADED, dateMavenMetadataTimestamp);
		log.info("Uploading to snapshots: {}", path);
		String pathToResourcesFile = "/example-project/" + ARTIFACT_FILE_NAME + ".jar";
		File file = ResourceUtils.getFile(this.getClass().getResource(pathToResourcesFile));
		byte[] content = Files.readAllBytes(file.toPath());
		uploadFile(content, path);
		log.info("Uploaded to snapshots: {}", path);
		
		path = format("%s/%s/%s-%s-1.pom", url, ARTIFACT_VERSION_SNAPSHOT, ARTIFACT_FILE_NAME_UPLOADED, dateMavenMetadataTimestamp);
		log.info("Uploading to snapshots: {}", path);
		pathToResourcesFile = "/example-project/" + "pom.xml";
		file = ResourceUtils.getFile(this.getClass().getResource("/example-project/pom.xml"));
		content = Files.readAllBytes(file.toPath());
		uploadFile(content, path);
		log.info("Uploaded to snapshots: {}", path);
		
		path = format("%s/maven-metadata.xml", url);
		log.info("Downloading from snapshots: {}", path);
		response = testRestTemplate.withBasicAuth(USERNAME, PASSWORD).getForEntity(path, String.class);
		log.info("Downloaded from snapshots: {}", path);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		
		String mavenMetaDataSnapshot = format(MAVEN_METADATA_SNAPSHOT, ARTIFACT_NAME, ARTIFACT_VERSION_SNAPSHOT, dateMavenMetadataTimestamp, dateMavenMetadata, ARTIFACT_VERSION, dateMavenMetadataTimestamp, dateMavenMetadata, ARTIFACT_VERSION, dateMavenMetadataTimestamp, dateMavenMetadata);
		path = format("%s/%s/maven-metadata.xml", url, ARTIFACT_VERSION_SNAPSHOT);
		log.info("Uploading to snapshots: {}", path);
		content = mavenMetaDataSnapshot.getBytes();
		uploadFile(content, path);
		log.info("Uploaded to snapshots: {}", path);

		String mavenMetaDataMain = format(MAVEN_METADATA_MAIN, ARTIFACT_NAME, ARTIFACT_VERSION_SNAPSHOT, dateMavenMetadata);
		path = format("%s/maven-metadata.xml", url, ARTIFACT_VERSION_SNAPSHOT);
		log.info("Uploading to snapshots: {}", path);
		content = mavenMetaDataMain.getBytes();
		uploadFile(content, path);
		log.info("Uploaded to snapshots: {}", path);
	}
	
	void uploadFile(byte[] content, String path) throws NoSuchAlgorithmException, URISyntaxException {
		String md5 = calculateMd5(content);
		String sha1 = calculateSha1(content);
		HttpEntity<byte[]> entity = new HttpEntity<byte[]>(content);
		ResponseEntity<String> response = testRestTemplate.withBasicAuth(USERNAME, PASSWORD).exchange(new URI(path), HttpMethod.PUT, entity, String.class);
		entity = new HttpEntity<byte[]>(md5.getBytes());
		response = testRestTemplate.withBasicAuth(USERNAME, PASSWORD).exchange(new URI(path + ".md5"), HttpMethod.PUT, entity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		entity = new HttpEntity<byte[]>(sha1.getBytes());
		response = testRestTemplate.withBasicAuth(USERNAME, PASSWORD).exchange(new URI(path + ".sha1"), HttpMethod.PUT, entity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		compareUploadedFileWithOriginal(content, path, md5, sha1);
	}
	
	String calculateMd5(byte[] content) throws NoSuchAlgorithmException {
		return calculateHash(content, "MD5");
	}
	
	String calculateSha1(byte[] content) throws NoSuchAlgorithmException {
		return calculateHash(content, "SHA-1");
	}
	
	String calculateHash(byte[] content, String hashAlgorithm) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
		md.update(content);
		byte[] digest = md.digest();
		return DatatypeConverter.printHexBinary(digest).toLowerCase();
	}
	
	void compareUploadedFileWithOriginal(byte[] originalContent, String path, String md5, String sha1) {
		log.info("Downloading from snapshots: {}", path);
		ResponseEntity<byte[]> response = testRestTemplate.withBasicAuth(USERNAME, PASSWORD).getForEntity(path, byte[].class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(new String(response.getBody())).isEqualTo(new String(originalContent));
		response = testRestTemplate.withBasicAuth(USERNAME, PASSWORD).getForEntity(path + ".md5", byte[].class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(md5.getBytes());
		response = testRestTemplate.withBasicAuth(USERNAME, PASSWORD).getForEntity(path + ".sha1", byte[].class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(sha1.getBytes());
	}
}
