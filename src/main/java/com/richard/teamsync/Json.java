package com.richard.teamsync;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Json
{
    public static String stringify(Object value)
    {
        if (value == null)
        {
            return "null";
        }
        if (value instanceof String)
        {
            return quote((String) value);
        }
        if (value instanceof Number || value instanceof Boolean)
        {
            return String.valueOf(value);
        }
        if (value instanceof Map)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            Iterator<? extends Map.Entry<?, ?>> it = ((Map<?, ?>) value).entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry<?, ?> e = it.next();
                sb.append(quote(String.valueOf(e.getKey()))).append(":").append(stringify(e.getValue()));
                if (it.hasNext()) sb.append(",");
            }
            sb.append("}");
            return sb.toString();
        }
        if (value instanceof List)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            Iterator<?> it = ((List<?>) value).iterator();
            while (it.hasNext())
            {
                sb.append(stringify(it.next()));
                if (it.hasNext()) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }
        return quote(String.valueOf(value));
    }

    private static String quote(String s)
    {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
