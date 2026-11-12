package org.csstudio.display.converter.edm;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.util.List;

import org.csstudio.display.actions.OpenDisplayAction;
import org.csstudio.display.actions.OpenDisplayAction.Target;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.opibuilder.converter.model.EdmModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** JUnit test for the target of converted related displays */
@SuppressWarnings("nls")
class RelatedDisplayTargetTest extends TestHelper
{
    private Target original_target;

    @BeforeEach
    void rememberTarget()
    {
        original_target = ConverterPreferences.related_display_target;
    }

    @AfterEach
    void restoreTarget()
    {
        ConverterPreferences.related_display_target = original_target;
    }

    @Test
    void testParseSetting()
    {
        assertThat(ConverterPreferences.parseRelatedDisplayTarget("standalone"), equalTo(Target.STANDALONE));
        assertThat(ConverterPreferences.parseRelatedDisplayTarget(" WINDOW "), equalTo(Target.WINDOW));
        assertThat(ConverterPreferences.parseRelatedDisplayTarget("tab"), equalTo(Target.TAB));
        assertThat(ConverterPreferences.parseRelatedDisplayTarget("new window"), equalTo(Target.TAB));
    }

    @Test
    void testDefaultTarget() throws Exception
    {
        ConverterPreferences.related_display_target = ConverterPreferences.parseRelatedDisplayTarget("tab");
        assertThat(convertTargets(), equalTo(List.of(Target.TAB, Target.REPLACE)));
    }

    @Test
    void testStandaloneTarget() throws Exception
    {
        ConverterPreferences.related_display_target = Target.STANDALONE;
        assertThat(convertTargets(), equalTo(List.of(Target.STANDALONE, Target.REPLACE)));
    }

    /** @return Targets of the actions of the converted related display button.
     *          The second related display has 'close display' set.
     */
    private List<Target> convertTargets() throws Exception
    {
        EdmModel.reloadEdmColorFile("colors.list", getClass().getResourceAsStream("/colors.list"));
        final File edl = new File(getClass().getResource("/related_display.edl").getFile());
        final EdmConverter converter = new EdmConverter(edl, null);
        final ActionButtonWidget button = (ActionButtonWidget) converter.getDisplayModel().getChildren().get(0);
        return button.propActions().getValue().getActions()
                     .stream()
                     .map(action -> ((OpenDisplayAction) action).getTarget())
                     .toList();
    }
}
