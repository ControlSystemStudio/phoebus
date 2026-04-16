package org.phoebus.applications.email.ui;

import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.phoebus.applications.email.EmailApp;
import org.phoebus.email.EmailPreferences;
import org.testfx.framework.junit5.ApplicationTest;

import java.io.IOException;

class EmailDialogControllerTestUI extends ApplicationTest {

    EmailDialogController controller;

    @Override
    public void start(Stage stage) throws IOException
    {
        final FXMLLoader loader = new FXMLLoader();
        loader.setLocation(EmailApp.class.getResource("ui/EmailDialog.fxml"));
        loader.load();
        controller = loader.getController();
    }

    @BeforeEach
    public void setup() {
        System.setProperty("test_key1", "test_value1");
        System.setProperty("test_key2", "test_value2");
        System.setProperty("test_recursive1", "$(test_recursive2)");
        System.setProperty("test_recursive2", "$(test_recursive1)");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("test_key1");
        System.clearProperty("test_key2");
        System.clearProperty("test_recursive1");
        System.clearProperty("test_recursive2");
    }

    @Test
    void testEmailAddressSubstitutions() {
        EmailPreferences.to = "$(test_key1)@place";
        EmailPreferences.from = "$(test_key2)@other";
        controller.initialize();

        Assertions.assertEquals("test_value1@place", controller.txtTo.getText());
        Assertions.assertEquals("test_value2@other", controller.txtFrom.getText());
    }

    @Test
    void testEmailAddressSubstitutionsWithIncorrectProperties() {
        EmailPreferences.to = "$(nonexistent)@place";
        EmailPreferences.from = "$(test_key2@other";
        controller.initialize();

        Assertions.assertEquals("$(nonexistent)@place", controller.txtTo.getText());
        Assertions.assertEquals("$(test_key2@other", controller.txtFrom.getText());
    }

    @Test
    void testEmailAddressSubstitutionsWithRecursiveProperties() {
        EmailPreferences.to = "$(test_recursive1)@place";
        EmailPreferences.from = "$(test_recursive2)@other";
        controller.initialize();

        Assertions.assertEquals("$(test_recursive1)@place", controller.txtTo.getText());
        Assertions.assertEquals("$(test_recursive2)@other", controller.txtFrom.getText());
    }
}
