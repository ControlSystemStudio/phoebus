package org.phoebus.applications.alarm.ui.tree.datetimepicker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

/**
 * A DateTimePicker with configurable datetime format where both date and time can be changed
 * via the text field and the date can additionally be changed via the JavaFX default date picker.
 * Modified from https://github.com/edvin/tornadofx-controls/blob/master/src/main/java/tornadofx/control/DateTimePicker.java
 */
@SuppressWarnings("unused")
public class DateTimePicker extends DatePicker {
    private static final String DefaultFormat = "yyyy-MM-dd HH:mm";

	private DateTimeFormatter formatter;
	private ObjectProperty<LocalDateTime> dateTimeValue = new SimpleObjectProperty<>(null);
	private ObjectProperty<String> format = new SimpleObjectProperty<String>() {
		@Override
        public void set(String newValue) {
			super.set(newValue);
			formatter = DateTimeFormatter.ofPattern(newValue);
		}
	};

	private void alignColumnCountWithFormat() {
		getEditor().setPrefColumnCount(getFormat().length());
	}

	/** Constructor */
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

	/** @return Time */
	public LocalDateTime getDateTimeValue() {
		return dateTimeValue.get();
	}

	/** @param dateTimeValue Time */
	public void setDateTimeValue(LocalDateTime dateTimeValue) {
		this.dateTimeValue.set(dateTimeValue);
	}

	private ObjectProperty<LocalDateTime> dateTimeValueProperty() {
		return dateTimeValue;
	}

	private String getFormat() {
		return format.get();
	}

	private ObjectProperty<String> formatProperty() {
		return format;
	}

	private void setFormat(String format) {
		this.format.set(format);
		alignColumnCountWithFormat();
	}

	class InternalConverter extends StringConverter<LocalDate> {
		@Override
        public String toString(LocalDate object) {
			LocalDateTime value = getDateTimeValue();
			return (value != null) ? value.format(formatter) : "";
		}

		@Override
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
