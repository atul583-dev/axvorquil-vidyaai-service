package com.axvorquil.vidya.context;

public class TenantContext {
    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();
    public static void set(String t)  { TENANT.set(t); }
    public static String get()        { return TENANT.get(); }
    public static void clear()        { TENANT.remove(); }
}
