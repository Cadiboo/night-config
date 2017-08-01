package com.electronwill.nightconfig.toml;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.io.CharacterInput;
import com.electronwill.nightconfig.core.io.CharsWrapper;
import com.electronwill.nightconfig.core.io.ConfigParser;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ReaderInput;
import com.electronwill.nightconfig.core.utils.FakeCommentedConfig;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A configurable parser of TOML configurations.
 *
 * @author TheElectronWill
 * @see <a href="https://github.com/toml-lang/toml">TOML specification</a>
 */
public final class TomlParser implements ConfigParser<TomlConfig, Config> {
	// --- Parser's settings ---
	private int initialStringBuilderCapacity = 16, initialListCapacity = 10;
	private boolean lenientBareKeys = false;

	// --- Parser's methods ---
	@Override
	public TomlConfig parse(Reader reader) {
		return parse(new ReaderInput(reader), new TomlConfig());
	}

	@Override
	public void parse(Reader reader, Config destination) {
		parse(new ReaderInput(reader), destination);
	}

	private <T extends Config> T parse(CharacterInput input, T destination) {
		CommentedConfig commentedConfig = FakeCommentedConfig.getCommented(destination);
		CommentedConfig rootTable = TableParser.parseNormal(input, this, commentedConfig);
		int next;
		while ((next = input.peek()) != -1) {
			final boolean isArray = (next == '[');
			if (isArray) {
				input.skipPeeks();
			}
			final List<String> path = TableParser.parseTableName(input, this, isArray);
			final int lastIndex = path.size() - 1;
			final String lastKey = path.get(lastIndex);
			final List<String> parentPath = path.subList(0, lastIndex);
			final Config parentConfig = getSubTable(rootTable, parentPath);
			final Map<String, Object> parentMap = (parentConfig != null) ? parentConfig.valueMap()
																		 : null;

			if (hasPendingComment()) {// Handles comments that are before the table declaration
				String comment = consumeComment();
				System.out.println("path: " + path);
				System.out.println("comment: \"" + comment + "\"");
				if (parentConfig instanceof CommentedConfig) {
					List<String> lastPath = Collections.singletonList(lastKey);
					((CommentedConfig)parentConfig).setComment(lastPath, comment);
				}
			}
			if (isArray) {// It's an element of an array of tables
				if (parentMap == null) {
					throw new ParsingException("Cannot create entry " + path + " because of an invalid " +
						"parent that isn't a table.");
				}
				TomlConfig table = TableParser.parseNormal(input, this);
				List<TomlConfig> arrayOfTables = (List)parentMap.get(lastKey);
				if (arrayOfTables == null) {
					arrayOfTables = createList();
					parentMap.put(lastKey, arrayOfTables);
				}
				arrayOfTables.add(table);
			} else {// It's a table
				if (parentMap == null) {
					throw new ParsingException("Cannot create entry " + path + " because of an invalid " +
						"parent that isn't a table.");
				}
				Object alreadyDeclared = parentMap.get(lastKey);
				if (alreadyDeclared == null) {
					TomlConfig table = TableParser.parseNormal(input, this);
					parentMap.put(lastKey, table);
				} else {
					if (alreadyDeclared instanceof Config) {
						Config table = (Config)alreadyDeclared;
						checkContainsOnlySubtables(table, path);
						CommentedConfig commentedTable = FakeCommentedConfig.getCommented(table);
						TableParser.parseNormal(input, this, commentedTable);
					} else {
						throw new ParsingException("Entry " + path + " has been defined twice.");
					}
				}
			}
		}
		return destination;
	}

	private Config getSubTable(Config parentTable, List<String> path) {
		if (path.isEmpty()) {
			return parentTable;
		}
		Config currentConfig = parentTable;
		for (String key : path) {
			Object value = currentConfig.valueMap().get(key);
			if (value == null) {
				Config sub = new TomlConfig();
				currentConfig.valueMap().put(key, sub);
				currentConfig = sub;
			} else if (value instanceof Config) {
				currentConfig = (Config)value;
			} else if (value instanceof List) {
				List<?> list = (List<?>)value;
				if (!list.isEmpty() && list.get(0) instanceof Config) {// Arrays of tables
					int lastIndex = list.size() - 1;
					currentConfig = ((Config)list.get(lastIndex));
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
		return currentConfig;
	}

	private void checkContainsOnlySubtables(Config table, List<String> path) {
		for (Object value : table.valueMap().values()) {
			if (!(value instanceof Config)) {
				throw new ParsingException("Table with path " + path + " has been declared twice.");
			}
		}
	}

	// --- Getters/setters for the settings ---
	public boolean isLenientWithBareKeys() {
		return lenientBareKeys;
	}

	public TomlParser setLenientWithBareKeys(boolean lenientBareKeys) {
		this.lenientBareKeys = lenientBareKeys;
		return this;
	}

	public TomlParser setInitialStringBuilderCapacity(int initialStringBuilderCapacity) {
		this.initialStringBuilderCapacity = initialStringBuilderCapacity;
		return this;
	}

	public TomlParser setInitialListCapacity(int initialListCapacity) {
		this.initialListCapacity = initialListCapacity;
		return this;
	}

	// --- Configured objects creation ---
	<T> List<T> createList() {
		return new ArrayList<>(initialListCapacity);
	}

	CharsWrapper.Builder createBuilder() {
		return new CharsWrapper.Builder(initialStringBuilderCapacity);
	}

	// --- Comment management ---
	private String currentComment;

	boolean hasPendingComment() {
		return currentComment != null;
	}

	String consumeComment() {
		String comment = currentComment;
		currentComment = null;
		return comment;
	}

	void setComment(CharsWrapper comment) {
		if(comment != null) {
			if(currentComment == null) {
				currentComment = comment.toString();
			} else {
				currentComment = currentComment + '\n' + comment.toString();
			}
		}
	}

	void setComment(List<CharsWrapper> commentsList) {
		CharsWrapper.Builder builder = new CharsWrapper.Builder(32);
		if (!commentsList.isEmpty()) {
			Iterator<CharsWrapper> it = commentsList.iterator();
			builder.append(it.next());
			while (it.hasNext()) {
				builder.append('\n');
				builder.append(it.next());
			}
			setComment(builder.build());// Appends the builder to the current comment if any
		}
	}
}