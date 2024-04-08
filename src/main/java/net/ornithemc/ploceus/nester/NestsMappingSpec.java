package net.ornithemc.ploceus.nester;

import net.fabricmc.loom.api.mappings.layered.MappingContext;
import net.fabricmc.loom.api.mappings.layered.spec.MappingsSpec;

import net.ornithemc.ploceus.PloceusGradleExtension;

public record NestsMappingSpec(PloceusGradleExtension ploceus) implements MappingsSpec<NestsMappingLayer> {

	@Override
	public int hashCode() {
		return "ploceus:nests".hashCode();
	}

	@Override
	public NestsMappingLayer createLayer(MappingContext context) {
		return new NestsMappingLayer(ploceus());
	}
}
