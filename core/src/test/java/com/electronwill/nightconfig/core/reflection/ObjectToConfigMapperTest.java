package com.electronwill.nightconfig.core.reflection;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.SimpleConfig;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author TheElectronWill
 */
public class ObjectToConfigMapperTest {

	@Test
	public void testWithSimpleConfig() throws Exception {
		ObjectToConfigMapper mapper = new ObjectToConfigMapper();
		Config config = new SimpleConfig();
		MyObject object = new MyObject();
		mapper.map(object, config);

		System.out.println("MyObject mapped to a SimpleConfig:");
		System.out.println(config.asMap());

		assert config.<Integer>getValue("integer") == object.integer;
		assert config.<Double>getValue("decimal") == object.decimal;
		assert config.<String>getValue("string") == object.string;
		assert config.<List<String>>getValue("stringList") == object.stringList;
		assert config.<Config>getValue("config") == object.config;
		assert config.getValue("subObject") instanceof Config;
		Config sub = (Config)config.getValue("subObject");
		assert sub.<Integer>getValue("integer") == 1234567890;
		assert sub.<Double>getValue("decimal") == Math.PI;
		assert sub.<String>getValue("string").equals("value");
		assert sub.<List<?>>getValue("stringList").equals(Arrays.asList("a", "b", "c"));
		assert sub.<Config>getValue("config").size() == 0;
		assert sub.containsValue("subObject");
		assert sub.getValue("subObject") == null;
	}

	@Test
	public void testWithMapConfig() throws Exception {
		ObjectToConfigMapper mapper = new ObjectToConfigMapper();
		Config config = new SimpleConfig(SimpleConfig.STRATEGY_SUPPORT_ALL);
		MyObject object = new MyObject();
		mapper.map(object, config);

		System.out.println("MyObject mapped to a MapConfig:");
		System.out.println(config.asMap());

		assert config.<Integer>getValue("integer") == object.integer;
		assert config.<Double>getValue("decimal") == object.decimal;
		assert config.<String>getValue("string") == object.string;
		assert config.<List<String>>getValue("stringList") == object.stringList;
		assert config.<Config>getValue("config") == object.config;
		assert config.getValue("subObject") == object.subObject;
	}

	private static class MyObject {
		int integer = 1234567890;
		double decimal = Math.PI;
		String string = "value";
		List<String> stringList = Arrays.asList("a", "b", "c");
		Config config = new SimpleConfig(SimpleConfig.STRATEGY_SUPPORT_ALL);
		MyObject subObject;

		public MyObject(MyObject subObject) {
			this.subObject = null;
			//empty config
		}

		public MyObject() {
			this.config.setValue("a.b.c", "configValue");
			this.subObject = new MyObject(null);
		}

		@Override
		public String toString() {
			return "MyObject{" +
				"integer=" + integer +
				", decimal=" + decimal +
				", string='" + string + '\'' +
				", stringList=" + stringList +
				", config=" + config +
				", subObject=" + subObject +
				'}';
		}
	}

}