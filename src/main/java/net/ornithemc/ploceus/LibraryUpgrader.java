package net.ornithemc.ploceus;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.vdurmont.semver4j.Semver;

import net.fabricmc.loom.configuration.providers.minecraft.library.Library;
import net.fabricmc.loom.configuration.providers.minecraft.library.Library.Target;
import net.fabricmc.loom.configuration.providers.minecraft.library.LibraryContext;
import net.fabricmc.loom.configuration.providers.minecraft.library.LibraryProcessor;
import net.fabricmc.loom.util.Platform;

public class LibraryUpgrader extends LibraryProcessor {

	private final Semver minecraftVersion;
	private final Set<Library> upgrades;

	public LibraryUpgrader(PloceusGradleExtension ploceus, Platform platform, LibraryContext context) {
		super(platform, context);

		this.minecraftVersion = new Semver(ploceus.normalizedMinecraftVersion());
		this.upgrades = new HashSet<>();

		if (ploceus.shouldUpgradeLibraries()) {
			addUpgrades();
		}
	}

	private void addUpgrades() {
		// the slf4j binding for log4j - this version has been tested to work on 1.6, 1.11, 1.12
		// lower versions of mc not tested because they did not yet ship log4j to begin with
		addUpgrade("org.slf4j:slf4j-api:2.0.1");
		addUpgrade("org.apache.logging.log4j:log4j-slf4j2-impl:2.19.0");
		addUpgrade("org.apache.logging.log4j:log4j-api:2.19.0");
		addUpgrade("org.apache.logging.log4j:log4j-core:2.19.0");
		addUpgrade("it.unimi.dsi:fastutil:8.5.9");
		addUpgrade("com.google.code.gson:gson:2.10");

		// logger-config is needed to make log4j work in versions prior to 13w39a
		addUpgrade("1.7.0-alpha.13.38.c", "net.ornithemc:logger-config:1.0.0");
		addUpgrade("1.5.2", "com.google.guava:guava:14.0");
		addUpgrade("1.7.5", "commons-codec:commons-codec:1.9");
		addUpgrade("1.7.10", "org.apache.commons:commons-compress:1.8.1");
		addUpgrade("1.5.2", "commons-io:commons-io:2.4");
		addUpgrade("1.5.2", "org.apache.commons:commons-lang3:3.1");
		addUpgrade("1.7.10", "commons-logging:commons-logging:1.1.3");
		addUpgrade("1.7.9", "org.apache.httpcomponents:httpcore:4.3.2");
		addUpgrade("1.7.9", "org.apache.httpcomponents:httpclient:4.3.3");
	}

	private void addUpgrade(String maven) {
		addUpgrade(null, null, maven);
	}

	private void addUpgrade(String maxMinecraftVersion, String maven) {
		addUpgrade(null, maxMinecraftVersion, maven);
	}

	private void addUpgrade(String minMinecraftVersion, String maxMinecraftVersion, String maven) {
		boolean minSatisfied = (minMinecraftVersion == null || minecraftVersion.compareTo(new Semver(minMinecraftVersion)) >= 0);
		boolean maxSatisfied = (maxMinecraftVersion == null || minecraftVersion.compareTo(new Semver(maxMinecraftVersion)) <= 0);

		if (minSatisfied && maxSatisfied) {
			upgrades.add(Library.fromMaven(maven, Target.COMPILE));
		}
	}

	@Override
	public ApplicationResult getApplicationResult() {
		return upgrades.isEmpty() ? ApplicationResult.DONT_APPLY : ApplicationResult.MUST_APPLY;
	}

	@Override
	public Predicate<Library> apply(Consumer<Library> dependencyConsumer) {
		for (Library upgrade : upgrades) {
			dependencyConsumer.accept(upgrade);
		}

		return ALLOW_ALL;
	}
}
