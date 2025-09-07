package it.unive.scsr;

public enum NumericalSize {
    INT8(-128.0, 127.0, "int8", false),
    UINT8(0.0, 255.0, "uint8", false),
    INT16(-32768.0, 32767.0, "int16", false),
    UINT16(0.0, 65535.0, "uint16", false),
    INT32(-2147483648.0, 2147483647.0, "int32", false),
    UINT32(0.0, 4294967295.0, "uint32", false),
    FLOAT8(-127.0, 127.0, "float8", true),
    FLOAT16(-65504.0, 65504.0, "float16", true),
    FLOAT32(-3.4028235e38, 3.4028235e38, "float32", true);

    private final double min;
    private final double max;
    private final String typeName;
    private final boolean isFloatingPoint;

    NumericalSize(double min, double max, String typeName, boolean isFloatingPoint) {
        this.min = min;
        this.max = max;
        this.typeName = typeName;
        this.isFloatingPoint = isFloatingPoint;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public String getTypeName() {
        return typeName;
    }

    public boolean isFloatingPoint() {
        return isFloatingPoint;
    }
}