/*******************************************************************************
 * Copyright (c) 2023 by CEA .
 * 
 * The full license specifying the redistribution, modification, usage and other rights 
 * and obligations is included with  the distribution of this project in the file "license.txt"
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN
 * 
 * THE IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE
 * ASSUMES NO RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE,
 * MODIFICATION,OR REDISTRIBUTION OF THIS SOFTWARE.
 ******************************************************************************/
package org.csstudio.display.builder.model.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetFactory;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
//import org.reflections.ReflectionUtils;
//import org.reflections.Reflections;
//import org.reflections.Store;

/**
 * This class generate the property list of all the existing widget in Phoebus
 * 
 * @author ksaintin
 *
 */
public class WidgetsInformationUtil {
	private static final String WIDGET_DESCRIPTOR = "WIDGET_DESCRIPTOR";

	public static enum PropInformation {
		name, description, type, category, value, defaultvalue, icon_url, widget_type, version, java_class
	};

	// Map <widget class, properties map and value>
	private static final Map<String, Map<String, Map<String, Object>>> WIDGET_MAP = new HashMap<>();

	private static Set<Class<? extends Widget>> getWidgetImplementations(String packageName) {
		
		Set<Class<? extends Widget>> widgetSet = new HashSet<>();
		if (packageName == null) {
			Set<WidgetDescriptor> widgetDescriptions = WidgetFactory.getInstance().getWidgetDescriptions();
			Widget widget = null;
			for (WidgetDescriptor desc : widgetDescriptions) {
				widget = desc.createWidget();
				widgetSet.add(widget.getClass());
			}
		}
		else {
			//Use Reflection API uncomment line in xml pom
			// Use Kay method to get the list of widget
			// TODO take in charge custom package
			// Reflections reflections = new Reflections(packageName);
			// Set<Class<? extends Widget>> widgetSet =
			// reflections.getSubTypesOf(Widget.class);
			// Set<Class<?>> allClassSet = reflections.getSubTypesOf(Object.class);
		}

		return widgetSet;
	}

	/**
	 * Return property information in a map
	 * 
	 * @param prop
	 * @return Map<information, value>
	 */
	private static Map<String, Object> getPropertyInformation(WidgetProperty<?> prop) {
		Map<String, Object> propInfo = new HashMap<>();
		if (prop != null) {
			propInfo.put(PropInformation.name.toString(), prop.getName());
			String description = prop.getDescription();
			if (description != null) {
				propInfo.put(PropInformation.description.toString(), description);
			}

			// TODO Manage a long description

			Object defaultValue = prop.getDefaultValue();
			if (defaultValue != null) {
				if (defaultValue.toString().contains("\n")) {
					defaultValue = defaultValue.toString().replace("\n", " ");
				}
				propInfo.put(PropInformation.defaultvalue.toString(), defaultValue);
				propInfo.put(PropInformation.type.toString(), defaultValue.getClass().getSimpleName());
			}

			Object value = prop.getValue();
			if (value != null && !value.toString().contains("null") && !value.toString().isEmpty()) {
				if (value.toString().contains("\n")) {
					value = value.toString().replace("\n", " ");
				}
				propInfo.put(PropInformation.value.toString(), value);
			}

			WidgetPropertyCategory category = prop.getCategory();
			if (category != null) {
				propInfo.put(PropInformation.category.toString(), category.name());
			}
		}
		return propInfo;
	}

	/**
	 * Return a map of widget Key classname Map of Property with value
	 */
	public static Map<String, Map<String, Map<String, Object>>> getAllWidgetInformations() {
		if (WIDGET_MAP.isEmpty()) {
			 getAllWidgetInformations(null);
			//String packageName = Widget.class.getPackage().getName() + ".widgets";
			//getAllWidgetInformations(packageName);
			//getAllWidgetInformations(packageName + ".plots");
		}
		return WIDGET_MAP;
	}

	private static void getAllWidgetInformations(String packageName) {
		Set<Class<? extends Widget>> widgetImplementations = getWidgetImplementations(packageName);
		Constructor<? extends Widget> declaredConstructor = null;
		Widget newInstance = null;
		Field descriptorField = null;

		Map<String, Map<String, Object>> propInformationMap = null;
		Map<String, Object> propertyInfos = null;
		for (Class<? extends Widget> widgetClass : widgetImplementations) {
			try {
				descriptorField = widgetClass.getDeclaredField(WIDGET_DESCRIPTOR);
			} catch (Exception e) {
				// e.printStackTrace();
			}
			if (descriptorField != null && !Modifier.isAbstract(widgetClass.getModifiers())) {

				try {
					declaredConstructor = widgetClass.getDeclaredConstructor();
					newInstance = declaredConstructor.newInstance();
				} catch (Exception e) {
					// System.out.println("widgetClass no empty constructor ");
				}

				if (newInstance == null) {
					try {
						declaredConstructor = widgetClass.getDeclaredConstructor(String.class);
						newInstance = declaredConstructor.newInstance();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (newInstance != null) {
					WidgetDescriptor descriptor = null;
					URL iconURL = null;
					try {
						Object object = descriptorField.get(newInstance);
						descriptor = ((WidgetDescriptor) object);
						iconURL = descriptor.getIconURL();
					} catch (Exception e) {
						// e.printStackTrace();
					}

					if (iconURL != null && descriptor != null) {

						propInformationMap = new HashMap<>();
						WIDGET_MAP.put(widgetClass.getSimpleName(), propInformationMap);
						// Add class
						propertyInfos = new HashMap<>();
						propertyInfos.put(PropInformation.name.toString(), PropInformation.java_class.toString());
						propertyInfos.put(PropInformation.type.toString(), String.class.getSimpleName());
						propertyInfos.put(PropInformation.description.toString(), "The java class of the widget");
						propertyInfos.put(PropInformation.value.toString(), widgetClass.getName());
						propInformationMap.put(PropInformation.java_class.toString(), propertyInfos);

						// Add widget type
						propertyInfos = new HashMap<>();
						propertyInfos.put(PropInformation.name.toString(), PropInformation.widget_type.toString());
						propertyInfos.put(PropInformation.type.toString(), String.class.getSimpleName());
						propertyInfos.put(PropInformation.description.toString(), "The type of the widget");
						propertyInfos.put(PropInformation.value.toString(), descriptor.getType());
						propInformationMap.put(PropInformation.widget_type.toString(), propertyInfos);
						// Add Icon
						propertyInfos = new HashMap<>();
						propertyInfos.put(PropInformation.name.toString(), PropInformation.icon_url.toString());
						propertyInfos.put(PropInformation.type.toString(), String.class.getSimpleName());
						propertyInfos.put(PropInformation.description.toString(), "The url icon path");
						String file = iconURL.getPath();
						File iconFile = new File(file);
						propertyInfos.put(PropInformation.value.toString(), "images/" + iconFile.getName());
						propInformationMap.put(PropInformation.icon_url.toString(), propertyInfos);
						// Add version
						propertyInfos = new HashMap<>();
						propertyInfos.put(PropInformation.name.toString(), PropInformation.version.toString());
						propertyInfos.put(PropInformation.type.toString(), String.class.getSimpleName());
						propertyInfos.put(PropInformation.description.toString(), "The version of the widget");
						propertyInfos.put(PropInformation.value.toString(), newInstance.getVersion());
						propInformationMap.put(PropInformation.version.toString(), propertyInfos);

						Set<WidgetProperty<?>> properties = newInstance.getProperties();
						if (properties != null) {
							for (WidgetProperty<?> prop : properties) {
								propertyInfos = getPropertyInformation(prop);
								propInformationMap.put(prop.getName(), propertyInfos);
							}
						}
					}
				}
			}
		}
	}

	public static void generateAllWidgetRSTFile(String generateFilePath) {
		if (generateFilePath != null) {
			Map<String, Map<String, Map<String, Object>>> allWidgetInformations = getAllWidgetInformations();

			Set<Entry<String, Map<String, Map<String, Object>>>> entrySet = allWidgetInformations.entrySet();
			String widgetName = null;
			Map<String, Map<String, Object>> propInfoMap = null;
			Collection<Map<String, Object>> propValues = null;
			// Set<Entry<String, Object>> propSet = null;
			File widgetDocument = new File(generateFilePath);

			// String widgetDocPath =
			// "D:\\tmp\\Code\\Phoebus\\PhoebusWidgetInformations.txt";

			// regenerate the makefile
			try ( // Try with resources in order to close at the end
					FileWriter fileWriter = new FileWriter(widgetDocument);
					BufferedWriter writer = new BufferedWriter(fileWriter);) {
				File parentFile = widgetDocument.getParentFile();
				// Create parent folder if not exist
				if (parentFile != null && !parentFile.exists()) {
					parentFile.mkdir();
				}

				StringBuilder sb = new StringBuilder();
				sb.append("=====================================\n");
				sb.append("Widget List and Associated Properties\n");
				sb.append("=====================================\n\n");

				for (Entry<String, Map<String, Map<String, Object>>> entry : entrySet) {
					widgetName = entry.getKey();
					propInfoMap = entry.getValue();
					propValues = propInfoMap.values();

					sb.append(widgetName + "\n");
					sb.append("========================\n\n");

					Map<String, Object> map = propInfoMap.get(PropInformation.icon_url.toString());
					sb.append(".. image:: " + map.get(PropInformation.value.toString()) + "\n");
					sb.append("\t:width: 30\n");
					sb.append("\t:alt: " + widgetName + "\n\n");
					map = propInfoMap.get(PropInformation.java_class.toString());
					sb.append("**Phoebus Class:** " + map.get(PropInformation.value.toString()) + "\n\n");
					map = propInfoMap.get(PropInformation.version.toString());
					sb.append("**Phoebus version:** " + map.get(PropInformation.value.toString()) + "\n\n");
					sb.append(".. list-table:: " + widgetName + " property list\n");
					sb.append("\t:header-rows: 1\n\n");
					sb.append("\t* - Name\n");
					sb.append("\t  - Description\n");
					sb.append("\t  - Type\n");
					sb.append("\t  - Category\n");
					sb.append("\t  - Value\n");
					sb.append("\t  - DefaultValue\n");

					for (Map<String, Object> propVal : propValues) {
						String propName = propVal.get(PropInformation.name.toString()).toString();
						Object cat = propVal.get(PropInformation.category.toString());
						if (cat != null) {
							sb.append("\t* - " + propName + "\n");
							sb.append("\t  - " + propVal.get(PropInformation.description.toString()) + "\n");
							Object typeObject = propVal.get(PropInformation.type.toString());
							Object type = typeObject != null && !typeObject.toString().contains("null") ? typeObject
									: "";
							sb.append("\t  - " + type + "\n");
							sb.append("\t  - " + propVal.get(PropInformation.category.toString()) + "\n");
							Object valObject = propVal.get(PropInformation.value.toString());
							Object val = valObject != null && !valObject.toString().contains("null") ? valObject : "";
							sb.append("\t  - " + val + "\n");
							Object defaultValObject = propVal.get(PropInformation.defaultvalue.toString());
							Object defaultVal = defaultValObject != null ? defaultValObject : "";
							sb.append("\t  - " + defaultVal + "\n");
						}
					}
					sb.append("\n\n");

				}

				writer.write(sb.toString());
				System.out.println("Rst document generatation success " + generateFilePath);

			} catch (Exception e) {
				System.err.println("Error during generation " + e.getMessage());
			}

		}

	}

	public static void displayAllWidgetInfo() {
		Map<String, Map<String, Map<String, Object>>> allWidgetInformations = getAllWidgetInformations();

		Set<Entry<String, Map<String, Map<String, Object>>>> entrySet = allWidgetInformations.entrySet();
		String widgetName = null;
		Map<String, Map<String, Object>> propInfoMap = null;
		Collection<Map<String, Object>> propValues = null;
		for (Entry<String, Map<String, Map<String, Object>>> entry : entrySet) {
			widgetName = entry.getKey();
			propInfoMap = entry.getValue();
			System.out.println("##### widget -> " + widgetName + "####");
			propValues = propInfoMap.values();
			for (Map<String, Object> propVal : propValues) {
				System.out.println(propVal);
			}
		}
	}

	/**
	 * Generate a rst document for all the widget and all the associated properties
	 * 
	 * @param args the generated rst file
	 */
	public static void main(String[] args) {

		String generateFilePath = args != null && args.length > 0 ? args[0] : null;

		if (generateFilePath == null) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setMultiSelectionEnabled(false);
			fileChooser.setControlButtonsAreShown(true);
			fileChooser.setDialogTitle("Select a directory in which the widgets information file will be generate ");
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			JFrame frame = new JFrame();
			frame.setAlwaysOnTop(true);
			int showOpenDialog = fileChooser.showOpenDialog(frame);
			File selected = showOpenDialog == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile() : null;
			generateFilePath = selected != null ? selected.getAbsolutePath() + File.separator + "widgets_properties.rst"
					: null;
		}

		if (generateFilePath != null) {
			generateAllWidgetRSTFile(generateFilePath);
		} else {
			System.err.println("No file or folder defined in argument");
		}

		System.exit(0);
	}
}
