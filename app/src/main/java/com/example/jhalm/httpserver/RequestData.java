package com.example.jhalm.httpserver;

import java.util.Dictionary;
import java.util.Hashtable;

public class RequestData {
    public String method;
    public String URI;
    private Hashtable<String, String> options = new Hashtable<String, String>();

    public boolean parseRequest(String request)
    {
        String array[] = request.split(" ");

        if(array.length == 3)
        {
            this.method = array[0];
            this.URI = array[1];

            return true;
        }

        return false;
    }

    public String getOption(String optionName)
    {
        if(this.options.containsKey(optionName))
        {
            return this.options.get(optionName);
        }

        return null;
    }

    public void parseOptions(String option)
    {
        String array[] = option.split("\\: ");

        if(array.length == 2)
        {
            this.options.put(array[0], array[1]);
        }
    }
}
