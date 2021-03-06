package fr.skyost.skydocs;

import fr.skyost.skydocs.exception.InvalidMenuDataException;
import fr.skyost.skydocs.exception.InvalidMenuEntryException;
import fr.skyost.skydocs.utils.IncludeFileFunction;
import fr.skyost.skydocs.utils.Utils;
import fr.skyost.skydocs.utils.Utils.AutoLineBreakStringBuilder;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.*;

/**
 * Represents a menu.
 */

public class DocsMenu {
	
	/**
	 * The language of this menu.
	 */
	
	private String language;
	
	/**
	 * Menu entries.
	 */
	
	private final List<DocsMenuEntry> entries = new ArrayList<>();
	
	/**
	 * Creates a new DocsMenu instance.
	 * 
	 * @param language Language of the menu.
	 * @param entries Entries of the menu.
	 */
	
	public DocsMenu(final String language, final DocsMenuEntry... entries) {
		this.language = language;
		this.entries.addAll(Arrays.asList(entries));
	}
	
	/**
	 * Gets the language of this menu.
	 * 
	 * @return The language of this menu.
	 */
	
	public final String getLanguage() {
		return language;
	}
	
	/**
	 * Sets the language of this menu.
	 * 
	 * @param language The new language of this menu.
	 */
	
	public final void setLanguage(final String language) {
		this.language = language;
	}
	
	/**
	 * Gets an entry by its link.
	 * 
	 * @param link Entry's link.
	 * 
	 * @return The entry.
	 */
	
	public final DocsMenuEntry getEntryByLink(final String link) {
		for(final DocsMenuEntry entry : entries) {
			if(entry.getLink().equals(link)) {
				return entry;
			}
		}
		return null;
	}
	
	/**
	 * Gets all entries from this menu.
	 * 
	 * @return All entries.
	 */
	
	public final List<DocsMenuEntry> getEntries() {
		return entries;
	}
	
	/**
	 * Adds entries to this menu.
	 * 
	 * @param entries Entries to add.
	 */
	
	public final void addEntries(final DocsMenuEntry... entries) {
		this.entries.addAll(Arrays.asList(entries));
	}
	
	/**
	 * Removes an entry from this menu.
	 * 
	 * @param name Entry's name.
	 */
	
	public final void removeEntry(final String name) {
		for(final DocsMenuEntry entry : entries) {
			if(entry.getTitle().equals(name)) {
				removeEntry(entry);
			}
		}
	}
	
	/**
	 * Removes an entry from this menu.
	 * 
	 * @param entry Entry to remove.
	 */
	
	public final void removeEntry(final DocsMenuEntry entry) {
		entries.remove(entry);
	}
	
	/**
	 * Clears entries from this menu.
	 */
	
	public final void clearEntries() {
		entries.clear();
	}
	
	/**
	 * Converts this menu to its HTML form.
	 * 
	 * @return The HTML content.
	 */
	
	public final String toHTML() {
		return toHTML(null);
	}
	
	/**
	 * Converts this menu to its HTML form.
	 * 
	 * @param page The page that is calling <strong>toHTML()</strong>.
	 * 
	 * @return The HTML content.
	 */
	
	protected final String toHTML(final DocsPage page) {
		final AutoLineBreakStringBuilder builder = new AutoLineBreakStringBuilder("<ul>");
		orderMenuEntries(getEntries());
		if(page == null) {
			for(final DocsMenuEntry entry : getEntries()) {
				builder.append(entry.toHTML());
			}
		}
		else {
			final JtwigModel model = page.getProject().getTemplate().createModel().with(Constants.VARIABLE_PAGE, page);
			
			final IncludeFileFunction includeFile = new IncludeFileFunction(page.getProject().getContentDirectory(), model, DocsTemplate.RANGE_FUNCTION);
			final EnvironmentConfiguration configuration = EnvironmentConfigurationBuilder.configuration().functions().add(includeFile).add(DocsTemplate.RANGE_FUNCTION).and().build();
			
			for(final DocsMenuEntry entry : getEntries()) {
				builder.append(JtwigTemplate.inlineTemplate(entry.toHTML(), configuration).render(model));
			}
		}
		builder.append(Utils.LINE_SEPARATOR + "</ul>");
		return builder.toString();
	}
	
	/**
	 * Creates a new menu from a file containing YAML menu data.
	 * 
	 * @param project The project (we need to know project default language).
	 * @param menuData The file containing menu data.
	 * 
	 * @return The parsed menu.
	 * 
	 * @throws InvalidMenuDataException If an error occurred while parsing the file.
	 */
	
	@SuppressWarnings("unchecked")
	public static DocsMenu createFromFile(final DocsProject project, final File menuData) throws InvalidMenuDataException {
		try {
			final DocsMenu menu = new DocsMenu(project.getDefaultLanguage());
			
			final String[] parts = Utils.separateFileHeader(menuData);
			String language = project.getDefaultLanguage();
			
			if(parts[0] != null) {
				final Map<String, Object> headers = Utils.decodeFileHeader(parts[0]);
				if(headers != null && headers.containsKey(Constants.KEY_HEADER_LANGUAGE)) {
					language = headers.get(Constants.KEY_HEADER_LANGUAGE).toString();
				}
			}
			menu.setLanguage(language);
			
			final Yaml yaml = new Yaml();
			final List<?> children = yaml.load(parts[1]);
			for(final Object child : children) {
				if(!(child instanceof HashMap)) {
					throw new InvalidMenuDataException("Invalid menu item (" + child.toString() + ").");
				}
				menu.addEntries(DocsMenuEntry.fromYAML((Map<String, Object>)child));
			}
			orderMenuEntries(menu.getEntries());
			
			return menu;
		}
		catch(final Exception ex) {
			throw new InvalidMenuDataException(ex);
		}
	}
	
	/**
	 * Order a list of menu entries by their weight.
	 * 
	 * @param entries Entries to order.
	 */
	
	public static void orderMenuEntries(final List<DocsMenuEntry> entries) {
		Collections.sort(entries);
	}
	
	/**
	 * Represents a menu entry.
	 */
	
	public static class DocsMenuEntry implements Comparable<DocsMenuEntry> {
		
		/**
		 * The title of this entry.
		 */
		
		private String title;
		
		/**
		 * The link of this entry.
		 */
		
		private String link;
		
		/**
		 * The weight of this entry.
		 */
		
		private int weight;
		
		/**
		 * If the link should be opened in a new tab.
		 */
		
		private boolean newTab;
		
		/**
		 * Children of this entry.
		 */
		
		private final List<DocsMenuEntry> children = new ArrayList<>();
		
		/**
		 * Creates a new DocsMenuEntry instance.
		 * 
		 * @param title Title of the entry.
		 * @param link Link attached to the entry.
		 * @param weight Weight of the entry.
		 * @param newTab Whether the entry should be opened in a new page.
		 * @param children Children of the entry.
		 */
		
		public DocsMenuEntry(final String title, final String link, final int weight, final boolean newTab, final DocsMenuEntry... children) {
			this.title = title;
			this.link = link;
			this.weight = weight;
			this.newTab = newTab;
			if(children != null) {
				this.children.addAll(Arrays.asList(children));
			}
		}
		
		@Override
		public final int compareTo(final DocsMenuEntry entry) {
			return Integer.compare(getWeight(), entry.getWeight());
		}
		
		/**
		 * Gets the title of this entry.
		 * 
		 * @return The title of this entry.
		 */
		
		public final String getTitle() {
			return title;
		}
		
		/**
		 * Sets the title of this entry.
		 * 
		 * @param title The new title of this entry.
		 */
		
		public final void setTitle(final String title) {
			this.title = title;
		}
		
		/**
		 * Gets the link attached to this entry.
		 * 
		 * @return The link attached to this entry.
		 */
		
		public final String getLink() {
			return link;
		}
		
		/**
		 * Attaches a link to this entry.
		 * 
		 * @param link The new link to attach to this entry.
		 */
		
		public final void setLink(final String link) {
			this.link = link;
		}
		
		/**
		 * Gets the weight of this entry.
		 * 
		 * @return The weight of this entry.
		 */
		
		public final int getWeight() {
			return weight;
		}
		
		/**
		 * Sets the weight of this entry.
		 * 
		 * @param weight The new weight of this entry.
		 */
		
		public final void setWeight(final int weight) {
			this.weight = weight;
		}
		
		/**
		 * Check whether the entry should be opened in a new page.
		 * 
		 * @return Whether the entry should be opened in a new page.
		 */
		
		public final boolean shouldOpenInNewTab() {
			return newTab;
		}
		
		/**
		 * Sets whether the entry should be opened in a new page.
		 * 
		 * @param newTab Whether the entry should be opened in a new page.
		 */
		
		public final void setOpenInNewTable(final boolean newTab) {
			this.newTab = newTab;
		}
		
		/**
		 * Adds children to this entry.
		 * 
		 * @param children Children to add.
		 */
		
		public final void addChildren(final DocsMenuEntry... children) {
			this.children.addAll(Arrays.asList(children));
		}
		
		/**
		 * Removes a child from this entry.
		 * 
		 * @param name Name of the child.
		 */
		
		public final void removeChild(final String name) {
			for(final DocsMenuEntry child : children) {
				if(child.getTitle().equals(name)) {
					removeChild(child);
				}
			}
		}
		
		/**
		 * Removes a child from this entry.
		 * 
		 * @param child Child to remove.
		 */
		
		public final void removeChild(final DocsMenuEntry child) {
			children.remove(child);
		}
		
		/**
		 * Clears children from this entry.
		 */
		
		public final void clearChildren() {
			children.clear();
		}
		
		/**
		 * Gets children of this entry.
		 * 
		 * @return Children of this entry.
		 */
		
		public final List<DocsMenuEntry> getChildren() {
			return children;
		}
		
		/**
		 * Converts this entry to its HTML form.
		 * 
		 * @return The HTML content.
		 */
		
		public final String toHTML() {
			final AutoLineBreakStringBuilder builder = new AutoLineBreakStringBuilder("<li>");
			builder.defaultAppend("<a href=\"" + getLink() + "\"");
			if(shouldOpenInNewTab()) {
				builder.defaultAppend(" target=\"_blank\"");
			}
			builder.append(">" + getTitle() + "</a>");
			if(children.size() > 0) {
				builder.append("<ul>");
				orderMenuEntries(children);
				for(final DocsMenuEntry child : children) {
					builder.append(child.toHTML());
				}
				builder.append("</ul>");
			}
			builder.append("</li>");
			return builder.toString();
		}
		
		/**
		 * Creates a new entry from an YAML object.
		 * 
		 * @param yamlObject The YAML object.
		 * 
		 * @return The parsed entry.
		 * 
		 * @throws InvalidMenuEntryException If an error occurred while reading the entry's data.
		 */
		
		@SuppressWarnings("unchecked")
		public static DocsMenuEntry fromYAML(final Map<String, Object> yamlObject) throws InvalidMenuEntryException {
			final DocsMenuEntry page = new DocsMenuEntry(null, "#", 0, false);
			
			if(!yamlObject.containsKey(Constants.KEY_MENU_TITLE)) {
				throw new InvalidMenuEntryException("Missing key \"" + Constants.KEY_MENU_TITLE + "\".");
			}
			page.setTitle(yamlObject.get(Constants.KEY_MENU_TITLE).toString());
			
			if(yamlObject.containsKey(Constants.KEY_MENU_LINK)) {
				page.setLink(yamlObject.get(Constants.KEY_MENU_LINK).toString());
			}
			
			if(yamlObject.containsKey(Constants.KEY_MENU_WEIGHT)) {
				final Integer weight = Utils.parseInt(yamlObject.get(Constants.KEY_MENU_WEIGHT).toString());
				page.setWeight(weight == null ? 0 : weight);
			}
			
			if(yamlObject.containsKey(Constants.KEY_MENU_NEW_TAB)) {
				final Boolean newTab = Utils.parseBoolean(yamlObject.get(Constants.KEY_MENU_NEW_TAB).toString());
				page.setOpenInNewTable(newTab == null ? false : newTab);
			}
			
			if(yamlObject.containsKey(Constants.KEY_MENU_CHILDREN)) {
				final List<?> children = (List<?>)yamlObject.get(Constants.KEY_MENU_CHILDREN);
				for(final Object child : children) {
					if(!(child instanceof HashMap)) {
						throw new InvalidMenuEntryException("Invalid child for \"" + page.getTitle() + "\" (" + child.toString() + ").");
					}
					page.addChildren(DocsMenuEntry.fromYAML((HashMap<String, Object>)child));
				}
				orderMenuEntries(page.getChildren());
			}
			
			return page;
		}
		
	}
	
}