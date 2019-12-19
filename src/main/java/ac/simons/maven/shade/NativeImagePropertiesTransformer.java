package ac.simons.maven.shade;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;

public class NativeImagePropertiesTransformer implements ResourceTransformer {

	private static final String NATIVE_IMAGE_PROPERTIES_RESOURCE_NAME = "native-image.properties";

	private static final List<String> CLASS_ARGUMENTS = Collections
		.unmodifiableList(Arrays.asList("--initialize-at-run-time", "--initialize-at-build-time"));

	private boolean transformedResource = false;

	private Map<String, Properties> dataMap = new HashMap<String, Properties>();

	@Override
	public boolean canTransformResource(String s) {
		return s.endsWith(NATIVE_IMAGE_PROPERTIES_RESOURCE_NAME);
	}

	@Override
	public void processResource(String s, InputStream inputStream, List<Relocator> list) throws IOException {
		System.out.println(System.identityHashCode(this) + ": Processing " + s);

		Properties properties = dataMap.computeIfAbsent(s, k -> new Properties());
		properties.load(inputStream);

		if (properties.containsKey("Args")) {
			String args = properties.getProperty("Args");
			List<String> modifiedArgs = new ArrayList<>();
			for (String arg : args.split("\\s+")) {
				String[] argAndValue = arg.split("=");
				if (argAndValue.length != 2 || !CLASS_ARGUMENTS.contains(argAndValue[0])) {
					modifiedArgs.add(arg);
					continue;
				}

				List<String> processedClassNames = new ArrayList<>();
				for (String className : argAndValue[1].split(",")) {
					boolean relocated = false;
					for (Relocator relocator : list) {
						if (relocator.canRelocateClass(className)) {
							processedClassNames.add(relocator.relocateClass(className));
							relocated = true;
							transformedResource = true;
							break;
						}
					}
					if (!relocated) {
						processedClassNames.add(className);
					}
				}
				modifiedArgs.add(String.format("%s=%s", argAndValue[0], String.join(",", processedClassNames)));
			}
			properties.setProperty("Args", String.join(" ", modifiedArgs));
		}
	}

	@Override
	public boolean hasTransformedResource() {
		System.out.println("have transformed?" + transformedResource);
		return !dataMap.isEmpty();
	}

	@Override
	public void modifyOutputStream(JarOutputStream jarOutputStream) throws IOException {
		dataMap.forEach((k, p) -> {
			try {
				System.out.println("STORING " + k);
				jarOutputStream.putNextEntry(new JarEntry(k));
				p.store(jarOutputStream, "asd");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

	}
}
