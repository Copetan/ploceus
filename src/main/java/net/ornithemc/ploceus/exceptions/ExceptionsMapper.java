package net.ornithemc.ploceus.exceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.objectweb.asm.commons.Remapper;

import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.mappingio.tree.MappingTree;

import net.ornithemc.exceptor.io.ClassEntry;
import net.ornithemc.exceptor.io.ExceptionsFile;
import net.ornithemc.exceptor.io.MethodEntry;
import net.ornithemc.ploceus.mappings.MappingsUtil;

public class ExceptionsMapper {

	private final MappingTree mappings;

	private ExceptionsFile excs;
	private Remapper remapper;

	public ExceptionsMapper(MappingTree mappings) {
		this.mappings = mappings;
	}

	public ExceptionsFile apply(ExceptionsFile excs, MappingsNamespace fromNs, MappingsNamespace toNs) {
		this.excs = excs;
		this.remapper = MappingsUtil.getAsmRemapper(mappings, fromNs, toNs);

		return remap();
	}

	private ExceptionsFile remap() {
		ExceptionsFile mappedExcs = new ExceptionsFile(new TreeMap<>());

		for (ClassEntry ci : excs.classes().values()) {
			String clsNameIn = ci.name();
			String clsNameOut = remapper.map(clsNameIn);

			ClassEntry co = new ClassEntry(clsNameOut, new TreeMap<>());
			mappedExcs.classes().put(co.name(), co);

			for (MethodEntry mi : ci.methods().values()) {
				String mtdNameIn = mi.name();
				String mtdDescIn = mi.descriptor();
				List<String> mtdExcsIn = mi.exceptions();
				String mtdNameOut = remapper.mapMethodName(clsNameIn, mtdNameIn, mtdDescIn);
				String mtdDescOut = remapper.mapMethodDesc(mtdDescIn);
				List<String> mtdExcsOut = new ArrayList<>(mtdExcsIn.size());
				for (String exc : mtdExcsIn) {
					mtdExcsOut.add(remapper.map(exc));
				}

				MethodEntry mo = new MethodEntry(mtdNameOut, mtdDescOut, mtdExcsOut);
				co.methods().put(mo.name() + mo.descriptor(), mo);
			}
		}

		return mappedExcs;
	}
}
