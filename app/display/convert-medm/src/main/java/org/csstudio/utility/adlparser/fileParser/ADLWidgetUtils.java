package org.csstudio.utility.adlparser.fileParser;

import java.util.ArrayList;

import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLBasicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLControl;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLDynamicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLLimits;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLMonitor;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLPoints;
import org.csstudio.utility.adlparser.fileParser.widgetParts.RelatedDisplayItem;
import org.csstudio.utility.adlparser.fileParser.widgets.*;

public class ADLWidgetUtils {
    public static Object[] getADLWidgetChildren(ADLWidget adlWidget){
        try {
            System.out.println("getADLWidgetChildren " + adlWidget.getType());
            if ( adlWidget.getType().equals("basic attribute")){
                return (new ADLBasicAttribute(adlWidget)).getChildren();
            }
            else if ( adlWidget.getType().equals("object")){
                return (new ADLObject(adlWidget)).getChildren();
            }
            else if ( adlWidget.getType().equals("control")){
                return (new ADLControl(adlWidget)).getChildren();
            }
            else if ( adlWidget.getType().equals("monitor")){
                return (new ADLMonitor(adlWidget)).getChildren();
            }
            else if ( adlWidget.getType().equals("points")){
                return (new ADLPoints(adlWidget)).getChildren();
            }
            else if ( adlWidget.getType().contains("display[")){
                return (new RelatedDisplayItem(adlWidget)).getChildren();
            }
            else if ( adlWidget.getType().equals("dynamic attribute")){
                return (new ADLDynamicAttribute(adlWidget)).getChildren();
            }
            else if ( adlWidget.getType().equals("limits")){
                System.out.println("--Processing limits");
                return (new ADLLimits(adlWidget)).getChildren();
            }

        }
        catch (WrongADLFormatException ex){
            System.out.println("problem parsing " + adlWidget.getType());
            ex.printStackTrace();
        }
        return adlWidget.getObjects().toArray();
    }

    public static Object[] adlWidgetArray2ObjectArray( ArrayList<ADLWidget> inWidget ){
        ArrayList<Object> objectList = new ArrayList<Object>();
        System.out.println("number of widgets in the ADLFile " + inWidget.size());
        for ( ADLWidget adlWidget : inWidget){
            try {
                String widgetType = adlWidget.getType();
                System.out.println("-"+widgetType);
                if (widgetType.equals("arc")){
                    objectList.add(new Arc(adlWidget));
                }
                else if (widgetType.equals("bar")){
                    objectList.add(new BarMonitor(adlWidget));

                }
                else if (widgetType.equals("byte")){
                    objectList.add(new ByteMonitor(adlWidget));

                }
                else if (widgetType.equals("cartesian plot")){
                    objectList.add(new CartesianPlot(adlWidget));

                }
                else if (widgetType.equals("choice button")){
                    objectList.add(new ChoiceButton(adlWidget));
                }
                else if (widgetType.equals("composite")){
                    objectList.add(new Composite(adlWidget));
                }
                else if (widgetType.equals("dynamic symbol")){

                }
                else if (widgetType.equals("file")){

                }
                else if (widgetType.equals("image")){
                    objectList.add(new Image(adlWidget));
                }
                else if (widgetType.equals("indicator")){
                    objectList.add(new Indicator(adlWidget));
                }
                else if (widgetType.equals("menu")){
                    objectList.add(new Menu(adlWidget));
                }
                else if (widgetType.equals("message button")){
                    objectList.add(new MessageButton(adlWidget));
                }
                else if (widgetType.equals("toggle button")){
                    objectList.add(new ToggleButton(adlWidget));
                }
                else if (widgetType.equals("meter")){
                    objectList.add(new Meter(adlWidget));
                }
                else if (widgetType.equals("oval")){
                    objectList.add(new Oval(adlWidget));
                }
                else if (widgetType.equals("polygon")){
                    objectList.add(new Polygon(adlWidget));
                }
                else if (widgetType.equals("polyline")){
                    objectList.add(new PolyLine(adlWidget));
                }
                else if (widgetType.equals("line")){
                    objectList.add(new Line(adlWidget));
                }
                else if (widgetType.equals("rectangle")){
                    objectList.add(new Rectangle(adlWidget));
                }
                else if (widgetType.equals("related display")){
                    objectList.add(new RelatedDisplay(adlWidget));
                }
                else if (widgetType.equals("strip chart")){
                    objectList.add(new StripChart(adlWidget));
                }
                else if (widgetType.equals("text")){
                    objectList.add(new TextWidget(adlWidget));
                }
                else if (widgetType.equals("text update")){
                    objectList.add(new TextUpdateWidget(adlWidget));
                }
                else if (widgetType.equals("text entry")){
                    objectList.add(new TextEntryWidget(adlWidget));
                }
                else if (widgetType.equals("valuator")){
                    objectList.add(new Valuator(adlWidget));
                }
                else if (widgetType.equals("basic attribute")){
                    objectList.add(new ADLBasicAttribute(adlWidget));
                }
                else if (widgetType.equals("dynamic attribute")){
                    objectList.add(new ADLDynamicAttribute(adlWidget));
                }
                else if (widgetType.equals("display")){
                    objectList.add(new ADLDisplay(adlWidget));
                }
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
        }
        System.out.println("size of objectList "+ objectList.size());
        return objectList.toArray();

    }
}
