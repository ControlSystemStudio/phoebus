package org.phoebus.applications.alarm.ui.tree.datetimepicker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;
import javafx.scene.control.TextFormatter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * A DateTimePicker with configurable datetime format where both date and time can be changed
 * via the text field and the date can additionally be changed via the JavaFX default date picker.
 * Modified from https://github.com/edvin/tornadofx-controls/blob/master/src/main/java/tornadofx/control/DateTimePicker.java
 */
@SuppressWarnings("unused")
public class DateTimePicker extends DatePicker {
	public static final String DefaultFormat = "yyyy-MM-dd HH:mm";

	private DateTimeFormatter formatter;
	private ObjectProperty<LocalDateTime> dateTimeValue = new SimpleObjectProperty<>(null);
	private ObjectProperty<String> format = new SimpleObjectProperty<String>() {
		public void set(String newValue) {
			super.set(newValue);
			formatter = DateTimeFormatter.ofPattern(newValue);
		}
	};

	public void alignColumnCountWithFormat() {
		getEditor().setPrefColumnCount(getFormat().length());
	}

	public DateTimePicker() {
		getStyleClass().add("datetime-picker");
		setFormat(DefaultFormat);
		setConverter(new InternalConverter());
        alignColumnCountWithFormat();
        getEditor().setTextFormatter(new TextFormatter<>(new InternalConverter()));

		// Syncronize changes to the underlying date value back to the dateTimeValue
		valueProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == null) {
				dateTimeValue.set(null);
			} else {
				if (dateTimeValue.get() == null) {
					dateTimeValue.set(LocalDateTime.of(newValue, LocalTime.now()));
				} else {
					LocalTime time = dateTimeValue.get().toLocalTime();
					dateTimeValue.set(LocalDateTime.of(newValue, time));
				}
			}
		});

		// Syncronize changes to dateTimeValue back to the underlying date value
		dateTimeValue.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                LocalDate dateValue = newValue.toLocalDate();
                boolean forceUpdate = dateValue.equals(valueProperty().get());
                // Make sure the display is updated even when the date itself wasn't changed
                setValue(dateValue);
                if (forceUpdate) setConverter(new InternalConverter());
            } else {
                setValue(null);
            }

        });

		// Persist changes onblur
		getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue)
				simulateEnterPressed();
		});

	}

	private void simulateEnterPressed() {
		getEditor().commitValue();
	}

	public LocalDateTime getDateTimeValue() {
		return dateTimeValue.get();
	}

	public void setDateTimeValue(LocalDateTime dateTimeValue) {
		this.dateTimeValue.set(dateTimeValue);
	}

	public ObjectProperty<LocalDateTime> dateTimeValueProperty() {
		return dateTimeValue;
	}

	public String getFormat() {
		return format.get();
	}

	public ObjectProperty<String> formatProperty() {
		return format;
	}

	public void setFormat(String format) {
		this.format.set(format);
		alignColumnCountWithFormat();
	}

	class InternalConverter extends StringConverter<LocalDate> {
		public String toString(LocalDate object) {
			LocalDateTime value = getDateTimeValue();
			return (value != null) ? value.format(formatter) : "";
		}

		public LocalDate fromString(String value) {
			if (value == null || value.isEmpty()) {
				dateTimeValue.set(null);
				return null;
			}

			dateTimeValue.set(LocalDateTime.parse(value, formatter));
			return dateTimeValue.get().toLocalDate();
		}
	}
}
