package net.ornithemc.ploceus.mappings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;

import net.fabricmc.loom.configuration.providers.mappings.IntermediaryMappingsProvider;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import net.ornithemc.ploceus.api.GameSide;

public abstract class CalamusGen1Provider extends IntermediaryMappingsProvider {

	public abstract Property<GameSide> getSide();

	@Override
	public void provide(Path tinyMappings, Project project) throws IOException {
		if (Files.exists(tinyMappings) && !getRefreshDeps().get()) {
			return;
		}

		if (getSide().get() == GameSide.MERGED) {
			super.provide(tinyMappings, project);
		} else {
			// gen1 mappings for pre-1.3 versions were provided as official -> intermediary
			// for the client and server environment separately, but Loom expects
			// them to be provided as intermediary -> [clientOfficial, serverOfficial] so
			// we must rewrite them before passing them to the intermediate mappings service

			String tmpPathName = "." + getName() + "-tmp.tiny";
			Path tmpPath = tinyMappings.resolveSibling(tmpPathName);

			super.provide(tmpPath, project);

			MemoryMappingTree mappings = new MemoryMappingTree();

			MappingWriter writer = MappingWriter.create(tinyMappings, MappingFormat.TINY_2_FILE);
			MappingNsRenamer renamer = new MappingNsRenamer(writer, Map.of("official", getSide().get() == GameSide.CLIENT ? "clientOfficial" : "serverOfficial"));
			MappingSourceNsSwitch nsSwitch = new MappingSourceNsSwitch(renamer, "intermediary");

			MappingReader.read(tmpPath, mappings);
			mappings.accept(nsSwitch);

			Files.delete(tmpPath);
		}
	}

	@Override
	public String getName() {
		return getSide().get().prefix() + "calamus-gen1" + super.getName();
	}
}
