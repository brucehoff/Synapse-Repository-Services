package org.sagebionetworks.schema.worker;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.schema.SynapseSchemaBootstrap;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class CreateJsonSchemaWorkerIntegrationTest {

	public static final long MAX_WAIT_MS = 1000 * 30;

	@Autowired
	AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;

	@Autowired
	JsonSchemaManager jsonSchemaManager;

	@Autowired
	private SynapseSchemaBootstrap schemaBootstrap;

	@Autowired
	UserManager userManager;

	UserInfo adminUserInfo;
	String organizationName;
	String schemaName;
	String semanticVersion;
	JsonSchema basicSchema;
	Organization organization;

	@BeforeEach
	public void before() {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		organizationName = "my.org.net";
		schemaName = "some.schema";
		semanticVersion = "1.1.1";
		CreateOrganizationRequest createOrgRequest = new CreateOrganizationRequest();
		createOrgRequest.setOrganizationName(organizationName);
		organization = jsonSchemaManager.createOrganziation(adminUserInfo, createOrgRequest);
		basicSchema = new JsonSchema();
		basicSchema.set$id(organizationName + "/" + schemaName + "/" + semanticVersion);
		basicSchema.setDescription("basic schema for integration test");
	}

	@AfterEach
	public void after() {
		jsonSchemaManager.truncateAll();
	}

	@Test
	public void testCreateSchema() throws InterruptedException {
		CreateSchemaRequest request = new CreateSchemaRequest();
		request.setSchema(basicSchema);
		CreateSchemaResponse response = asynchronousJobWorkerHelper.startAndWaitForJob(adminUserInfo, request,
				MAX_WAIT_MS, CreateSchemaResponse.class);
		assertNotNull(response);
		assertNotNull(response.getNewVersionInfo());
		assertEquals(adminUserInfo.getId().toString(), response.getNewVersionInfo().getCreatedBy());
		assertEquals(semanticVersion, response.getNewVersionInfo().getSemanticVersion());
		jsonSchemaManager.deleteSchemaAllVersion(adminUserInfo, organizationName, schemaName);
		assertThrows(NotFoundException.class, () -> {
			jsonSchemaManager.deleteSchemaAllVersion(adminUserInfo, organizationName, schemaName);
		});
	}

	public void registerSchemaFromClasspath(String name) throws Exception {
		try (InputStream in = CreateJsonSchemaWorkerIntegrationTest.class.getClassLoader().getResourceAsStream(name);) {
			if (in == null) {
				throw new IllegalArgumentException("Cannot find: '" + name + "' on the classpath");
			}
			String json = IOUtils.toString(in, "UTF-8");
			JsonSchema schema = EntityFactory.createEntityFromJSONString(json, JsonSchema.class);
			CreateSchemaRequest request = new CreateSchemaRequest();
			request.setSchema(schema);
			System.out.println("Creating schema: '" + schema.get$id() + "'");
			CreateSchemaResponse response = asynchronousJobWorkerHelper.startAndWaitForJob(adminUserInfo, request,
					MAX_WAIT_MS, CreateSchemaResponse.class);
			System.out.println(response.getNewVersionInfo());
		}
	}

	@Test
	public void testMainUseCase() throws Exception {
		jsonSchemaManager.truncateAll();
		schemaBootstrap.bootstrapSynapseSchemas();
		CreateOrganizationRequest createOrgRequest = new CreateOrganizationRequest();
		createOrgRequest.setOrganizationName("my.organization");
		organization = jsonSchemaManager.createOrganziation(adminUserInfo, createOrgRequest);
		String[] schemasToRegister = { "pets/PetType.json", "pets/Pet.json", "pets/CatBreed.json", "pets/DogBreed.json",
				"pets/Cat.json", "pets/Dog.json", "pets/PetPhoto.json" };
		for (String fileName : schemasToRegister) {
			registerSchemaFromClasspath(fileName);
		}

		JsonSchema validationSchema = jsonSchemaManager.getValidationSchema("my.organization/pets.PetPhoto");
		assertNotNull(schemaBootstrap);
		printJson(validationSchema);
		assertNotNull(validationSchema.get$defs());
		assertTrue(validationSchema.get$defs().containsKey("#/$defs/my.organization/pets.PetType"));
		assertTrue(validationSchema.get$defs().containsKey("#/$defs/my.organization/pets.Pet"));
		assertTrue(validationSchema.get$defs().containsKey("#/$defs/my.organization/pets.Pet/1.0.3"));
		assertTrue(validationSchema.get$defs().containsKey("#/$defs/my.organization/pets.dog.Breed"));
		assertTrue(validationSchema.get$defs().containsKey("#/$defs/my.organization/pets.cat.Breed"));
		assertTrue(validationSchema.get$defs().containsKey("#/$defs/my.organization/pets.cat.Cat"));
		assertTrue(validationSchema.get$defs().containsKey("#/$defs/my.organization/pets.dog.Dog"));
		assertTrue(validationSchema.get$defs().containsKey("#/$defs/org.sagebionetworks/repo.model.Entity"));
		assertTrue(validationSchema.get$defs().containsKey("#/$defs/org.sagebionetworks/repo.model.Versionable"));
		assertTrue(validationSchema.get$defs().containsKey("#/$defs/org.sagebionetworks/repo.model.VersionableEntity"));
		assertTrue(validationSchema.get$defs().containsKey("#/$defs/org.sagebionetworks/repo.model.FileEntity"));

	}

	public void printJson(JSONEntity entity) throws JSONException, JSONObjectAdapterException {
		JSONObject object = new JSONObject(EntityFactory.createJSONStringForEntity(entity));
		System.out.println(object.toString(5));
	}
}
