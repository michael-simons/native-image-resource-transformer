package ac.simons.maven.shade;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class NativeImagePropertiesTransformerTest {

	@Test
	void multipleArgs() throws IOException {

		InputStream inputStream = NativeImagePropertiesTransformer.class.getResourceAsStream("/multiple-args.properties");
		new NativeImagePropertiesTransformer().processResource("", inputStream, Collections.emptyList());
	}
}