package com.electronwill.nightconfig.core.check;

import com.electronwill.nightconfig.core.AttributeType;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.StandardAttributes;
import com.electronwill.nightconfig.core.utils.ConfigWrapper;
import com.electronwill.nightconfig.core.utils.TransformingMap;
import com.electronwill.nightconfig.core.utils.TransformingSet;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * A configuration that checks every modification (add, set, remove) with a {@link ConfigChecker}.
 */
public class CheckedConfig extends ConfigWrapper<Config> {
	protected final ConfigChecker checker;

	/**
	 * Creates a new CheckedConfig and checks the existing elements with the checker.
	 * @param config the config to use as a data storage
	 * @param checker will check the config's data
	 */
	public CheckedConfig(Config config, ConfigChecker checker) {
		super(config);
		this.checker = checker;

		// The config might already contain some elements and we must be sure that
		// they are all correct
		Deque<String> path = new LinkedList<>();
		config.valueMap().forEach((k, v) -> {
			path.add(k);
			recursiveCheck(path, v);
			path.remove();
		});
	}

	private void recursiveCheck(Deque<String> path, Object value) {
		if (value instanceof Config) {
			for (Config.Entry entry : ((Config)value).entries()) {
				path.addLast(entry.getKey());
				recursiveCheck(path, entry.getValue());
				path.removeLast();
			}
		} else {
			String[] arr = (String[])path.toArray();
			checker.checkUpdate(StandardAttributes.VALUE, arr, value, value);
		}
	}

	@Override
	public <T> T set(AttributeType<T> attribute, String[] path, T value) {
		checker.checkUpdate(attribute, path, config.get(path), value);
		return config.set(attribute, path, value);
	}

	@Override
	public <T> T add(AttributeType<T> attribute, String[] path, T value) {
		checker.checkUpdate(StandardAttributes.VALUE, path, null, value);
		return config.add(attribute, path, value);
	}

	@Override
	public <T> T remove(AttributeType<T> attribute, String[] path) {
		checker.checkUpdate(attribute, path, config.get(path), null);
		return config.remove(attribute, path);
	}

	@Override
	public Map<String, Object> valueMap() {
		return new TransformingMap<>(super.valueMap(), (k,v) -> v, this::checkMapWrite, o -> o);
	}

	@Override
	public Set<Config.Entry> entries() {
		return new TransformingSet<>(config.entries(), v -> v, this::checkSetWrite, this::searchEntryTransform);
	}

	private <T> T checkMapWrite(String key, T value) {
		String[] path = { key };
		checker.checkUpdate(StandardAttributes.VALUE, path, get(path), value);
		return value;
	}

	private Config.Entry checkSetWrite(Config.Entry entry) {
		return checkMapWrite(entry.getKey(), entry.getValue());
	}

	private Config.Entry searchEntryTransform(Object o) {
		if (o instanceof Config.Entry) {
			return (Config.Entry)o;
		}
		return null;
	}
}
