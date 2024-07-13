package net.ornithemc.ploceus.exceptions;

import java.io.IOException;
import java.nio.file.Path;

import javax.inject.Inject;

import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.api.processor.MinecraftJarProcessor;
import net.fabricmc.loom.api.processor.ProcessorContext;
import net.fabricmc.loom.api.processor.SpecContext;
import net.fabricmc.mappingio.tree.MappingTree;

import net.ornithemc.exceptor.Exceptor;
import net.ornithemc.exceptor.io.ExceptionsFile;
import net.ornithemc.ploceus.PloceusGradleExtension;

public class ExceptionPatcherProcessor implements MinecraftJarProcessor<ExceptionPatcherProcessor.Spec> {

	private final PloceusGradleExtension ploceus;

	@Inject
	public ExceptionPatcherProcessor(PloceusGradleExtension ploceus) {
		this.ploceus = ploceus;
	}

	@Override
	public String getName() {
		return "ploceus:exception_patcher";
	}

	@Override
	public Spec buildSpec(SpecContext context) {
		ExceptionsProvider excs = ploceus.getExceptionsProvider();
		return excs.isPresent() ? new Spec(excs) : null;
	}

	@Override
	public void processJar(Path jar, Spec spec, ProcessorContext ctx) throws IOException {
		try {
			MappingTree mappings = ctx.getMappings();
			ExceptionsFile excs = ploceus.getExceptionsProvider().get(mappings, MappingsNamespace.NAMED);

			Exceptor.apply(jar, excs);
		} catch (IOException e) {
			throw new IOException("failed to patch exceptions!", e);
		}
	}

	public static class Spec implements MinecraftJarProcessor.Spec {

		private final ExceptionsProvider excs;

		private Integer hashCode;

		public Spec(ExceptionsProvider excs) {
			this.excs = excs;
		}

		@Override
		public int hashCode() {
			if (hashCode == null) {
				hashCode = excs.hashCode();
			}

			return hashCode;
		}
	}
}
