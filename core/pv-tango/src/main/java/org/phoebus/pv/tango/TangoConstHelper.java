package org.phoebus.pv.tango;

import java.lang.reflect.Field;

import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.TangoDs.TangoConst;

/** 
*  Tango constants helper
*
*  @author katy.saintin@cea.fr
*/

public class TangoConstHelper {
    public final static int SCALAR_TYPE = AttrDataFormat._SCALAR;
    public final static int ARRAY_TYPE = AttrDataFormat._SPECTRUM;
    public final static int IMAGE_TYPE = AttrDataFormat._IMAGE;
    public final static int UNKNOW_TYPE = AttrDataFormat._FMT_UNKNOWN;

    public final static int BOOLEAN_FORMAT = 1;
    public final static int NUMERICAL_FORMAT = 2;
    public final static int STRING_FORMAT = 3;
    public final static int UNKNOW_FORMAT = 4;

    private static final String BOOLEAN = "BOOLEAN";
    private static final String UNKNOW_TYPE_STR = "Unknow type";

    public static final String DOUBLE_NAME = "DOUBLE";
    public static final String LONG_NAME = "LONG";
    public static final String SHORT_NAME = "SHORT";
    public static final String CHAR_NAME = "CHAR";
    public static final String INT_NAME = "INT";
    public static final String FLOAT_NAME = "FLOAT";
    public static final String ENUM_NAME = "ENUM";
    public static final String ENCODED_NAME = "ENCODED";
    public static final String ARRAY_NAME = "ARRAY";
    
    private final static String[] NUMERICAL_LABEL = new String[] { DOUBLE_NAME, LONG_NAME, SHORT_NAME, INT_NAME, FLOAT_NAME, CHAR_NAME,ENUM_NAME,
            ENCODED_NAME };

    public static final String STATE_NAME = "STATE";
    public static final String STATUS_NAME = "STATUS";
    private final static String[] STRING_LABEL = new String[] { "STRING", STATE_NAME, STATUS_NAME};

    /**
     * Return simple type according a TangoConst_DEV_* type. *
     * 
     * @param TangoConst_DEV type
     * @return the type that can be SCALAR_TYPE, ARRAY_TYPE or UNKNOW_TYPE
     * @see TangoConst class in TangORB
     */

    public static int getTangoType(int tangoType) {
        int type = TangoConstHelper.UNKNOW_TYPE;
        Field[] fieldList = TangoConst.class.getFields();
        Field tmpField = null;
        String tmpFieldName = null;
        int tmpFieldValue = 0;
        for (Field element : fieldList) {
            tmpField = element;
            tmpFieldName = tmpField.getName();
            if (isATangoType(tmpFieldName)) {
                try {
                    tmpFieldValue = tmpField.getInt(tmpField);
                } catch (Exception e) {
                    type = TangoConstHelper.UNKNOW_TYPE;
                    break;
                }
                if (tangoType == tmpFieldValue) {
                    if (tmpFieldName.endsWith(ARRAY_NAME)) {
                        type = TangoConstHelper.ARRAY_TYPE;
                        break;
                    } else {
                        type = TangoConstHelper.SCALAR_TYPE;
                        break;
                    }
                }
            }
        }
        return type;
    }
    
    public static String getTangoFieldName(int tangoType) {
        String fieldName = null;
        Field[] fieldList = TangoConst.class.getFields();
        Field tmpField = null;
        String tmpFieldName = null;
        int tmpFieldValue = 0;
        for (Field element : fieldList) {
            tmpField = element;
            tmpFieldName = tmpField.getName();
            if (isATangoType(tmpFieldName)) {
                try {
                    tmpFieldValue = tmpField.getInt(tmpField);
                } catch (Exception e) {
                    fieldName = null;
                    break;
                }
                if (tangoType == tmpFieldValue) {
                    fieldName = tmpFieldName ;
                    break;
                }
            }
        }
        return fieldName;
    }

    private static boolean isATangoType(String tangoFieldName) {
        return ((tangoFieldName != null) && tangoFieldName.startsWith("Tango_DEV"));
    }

    private static boolean isString(String tangoFieldName) {
        boolean result = false;
        if (isATangoType(tangoFieldName)) {
            for (String element : STRING_LABEL) {
                if (tangoFieldName.contains(element)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    private static boolean isBoolean(String tangoFieldName) {
        boolean result = false;
        if (isATangoType(tangoFieldName) && tangoFieldName.contains(BOOLEAN)) {
            result = true;
        }
        return result;

    }

    private static boolean isNumerical(String tangoFieldName) {
        boolean result = false;
        if (isATangoType(tangoFieldName)) {
            for (String element : NUMERICAL_LABEL) {
                if (tangoFieldName.contains(element)) {
                    result = true;
                    break;
                }
            }
        }
        return result;

    }

    public static int getTangoFormat(int tangoType) {
        int format = TangoConstHelper.UNKNOW_FORMAT;
        Field[] fieldList = TangoConst.class.getFields();
        Field tmpField = null;
        String tmpFieldName = null;
        int tmpFieldValue = 0;
        for (Field element : fieldList) {
            tmpField = element;
            tmpFieldName = tmpField.getName();
            if (isATangoType(tmpFieldName)) {
                try {
                    tmpFieldValue = tmpField.getInt(tmpField);
                } catch (Exception e) {
                    format = TangoConstHelper.UNKNOW_TYPE;
                    break;
                }
                if (tangoType == tmpFieldValue) {
                    if (isBoolean(tmpFieldName)) {
                        format = TangoConstHelper.BOOLEAN_FORMAT;
                        break;
                    } else if (isString(tmpFieldName)) {
                        format = TangoConstHelper.STRING_FORMAT;
                        break;
                    } else if (isNumerical(tmpFieldName)) {
                        format = TangoConstHelper.NUMERICAL_FORMAT;
                        break;
                    }
                }
            }
        }
        return format;
    }

    public static int getAttributeTangoFormat(int tangoType) {
        int format = TangoConstHelper.UNKNOW_FORMAT;
        Field[] fieldList = TangoConst.class.getFields();
        Field tmpField = null;
        String tmpFieldName = null;
        int tmpFieldValue = 0;
        for (Field element : fieldList) {
            tmpField = element;
            tmpFieldName = tmpField.getName();
            if (isATangoType(tmpFieldName)) {
                try {
                    tmpFieldValue = tmpField.getInt(tmpField);
                } catch (Exception e) {
                    format = TangoConstHelper.UNKNOW_TYPE;
                    break;
                }
                if (tangoType == tmpFieldValue) {
                    if (isBoolean(tmpFieldName)) {
                        format = TangoConstHelper.BOOLEAN_FORMAT;
                        break;
                    } else if (isString(tmpFieldName)) {
                        format = TangoConstHelper.STRING_FORMAT;
                        break;
                    } else if (isNumerical(tmpFieldName)) {
                        format = TangoConstHelper.NUMERICAL_FORMAT;
                        break;
                    }
                }
            }
        }
        return format;
    }

    public static String getTangoTypeName(int tangotype) {
        String type = UNKNOW_TYPE_STR;
        Field[] fieldList = TangoConst.class.getFields();
        Field tmpField = null;
        String tmpFieldName = null;
        int tmpFieldValue = 0;
        for (Field element : fieldList) {
            tmpField = element;
            tmpFieldName = tmpField.getName();
            if (isATangoType(tmpFieldName)) {
                try {
                    tmpFieldValue = tmpField.getInt(tmpField);
                } catch (Exception e) {
                    type = UNKNOW_TYPE_STR;
                    break;
                }
                if (tangotype == tmpFieldValue) {
                    type = tmpFieldName;
                    break;
                }
            }
        }
        return type;
    }
    
  
}

