package net.ornithemc.ploceus;

import java.util.Map;

import org.gradle.api.Project;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.api.mappings.layered.spec.FileSpec;
import net.fabricmc.loom.configuration.DependencyInfo;

import net.ornithemc.ploceus.mappings.SidedIntermediaryMappingsProvider;
import net.ornithemc.ploceus.mcp.McpMappingsSpec;
import net.ornithemc.ploceus.nester.NestedMappingsSpec;
import net.ornithemc.ploceus.nester.NesterProcessor;
import net.ornithemc.ploceus.nester.NestsProvider;

public class PloceusGradleExtension {

	public static PloceusGradleExtension get(Project project) {
		return (PloceusGradleExtension)project.getExtensions().getByName("ploceus");
	}

	private final Project project;
	private final LoomGradleExtension loom;
	private final OslVersionCache oslVersions;

	public PloceusGradleExtension(Project project) {
		this.project = project;
		this.loom = LoomGradleExtension.get(this.project);
		this.oslVersions = new OslVersionCache(this.project);

		apply();
	}

	private void apply() {
		project.getConfigurations().register(Constants.NESTS_CONFIGURATION);
		project.getExtensions().getExtraProperties().set(Constants.VERSION_MANIFEST_PROPERTY, Constants.VERSION_MANIFEST_URL);

		loom.addMinecraftJarProcessor(NesterProcessor.class, this);
		setIntermediaryProvider(GameSide.MERGED);
	}

	public NestsProvider getNestsProvider() {
		return NestsProvider.of(project);
	}

	public NestedMappingsSpec nestedMappings() {
		return new NestedMappingsSpec(this);
	}

	public McpMappingsSpec mcpMappings(String channel, String build) {
		return mcpMappings(channel, DependencyInfo.create(project, Constants.MINECRAFT_CONFIGURATION).getDependency().getVersion(), build);
	}

	public McpMappingsSpec mcpMappings(String channel, String mc, String build) {
		return new McpMappingsSpec(
			FileSpec.create(String.format(Constants.MCP_MAVEN_GROUP + ":" + Constants.SRG_MAPPINGS, mc)),
			FileSpec.create(String.format(Constants.MCP_MAVEN_GROUP + ":" + Constants.MCP_MAPPINGS, channel, build, mc))
		);
	}

	public void dependOsl(String version) throws Exception {
		dependOsl(version, GameSide.MERGED);
	}

	public void dependOsl(String version, String side) throws Exception {
		dependOsl(version, GameSide.of(side));
	}

	public void dependOsl(String version, GameSide side) throws Exception {
		for (Map.Entry<String, String> entry : oslVersions.getDependencies(version).entrySet()) {
			String module = entry.getKey();
			String baseVersion = entry.getValue();
			String moduleVersion = oslVersions.getVersion(module, baseVersion, side);

			// not all modules cover all Minecraft versions
			// so check if a valid module version exists for
			// this Minecraft version before adding the dependency
			if (moduleVersion != null) {
				addOslModuleDependency(module, moduleVersion);
			}
		}
	}

	public void dependOslModule(String module, String version) throws Exception {
		dependOslModule(module, version, GameSide.MERGED);
	}

	public void dependOslModule(String module, String version, String side) throws Exception {
		dependOslModule(module, version, GameSide.of(side));
	}

	public void dependOslModule(String module, String version, GameSide side) throws Exception {
		String moduleVersion = oslVersions.getVersion(module, version, side);

		if (moduleVersion == null) {
			throw new RuntimeException("module " + module + " version " + version + " does not exist!");
		} else {
			addOslModuleDependency(module, moduleVersion);
		}
	}

	private void addOslModuleDependency(String module, String version) {
		project.getDependencies().add("modImplementation", String.format("%s:%s:%s",
			Constants.OSL_MAVEN_GROUP,
			module,
			version));
	}

	public void clientOnlyMappings() {
		setIntermediaryProvider(GameSide.CLIENT);
	}

	public void serverOnlyMappings() {
		setIntermediaryProvider(GameSide.SERVER);
	}

	private void setIntermediaryProvider(GameSide side) {
		loom.getIntermediaryUrl().convention(Constants.CALAMUS_INTERMEDIARY_URL.replace("%1$s", "%1$s" + side.suffix()));

		loom.setIntermediateMappingsProvider(SidedIntermediaryMappingsProvider.class, provider -> {
			provider.configure(project, loom, side);
		});
	}
}
