import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class HTTPRequest
{
    RequestType type;
    String resource;
    HTTPHeader headers[];

    String getHeaderValue(String key)
    {
        for (int i = 0; i < headers.length; i++)
        {
            if (headers[i].key.equals(key))
                return headers[i].value;
        }

        return null;
    }
}

class HTTPHeader {
    String key;
    String value;
}

enum RequestType {
    GET,
    POST
}

class Timer implements Runnable
{
    boolean isComplete = true;

    public Timer(int time)
    {
        isComplete = false;
    }

    public void run()
    {
        try
        {
            System.out.println("timer sleeping for " + Server.time + "ms...");
            Thread.sleep(Server.time);
        }
        catch (Exception e)
        {
            System.err.println("Timer interrupted.");
        }
        System.out.println("timer finished.");
        Server.setCatStatus(false);
    }
}

public class Server {

    static String resp404 = "<html><body><h1>404: Page not found</h3><a href=\"/\">Return to root</a></body></html>";
    static Timer catReset = new Timer(20000);
    static int time = 20000;
    private static boolean catSpotted = false;
    
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new HanRoot());
        server.createContext("/res", new HanRes());
        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Server running...");
    }

    static boolean getCatStatus()
	{
		return catSpotted;
	}

	static void setCatStatus(boolean cat)
	{
		System.out.println("setting cat to: " + cat);
		catSpotted = cat;
	}

    static class HanRoot implements HttpHandler
    {
        HttpExchange t;

        public void handle(HttpExchange httpEx) throws IOException
        {
            this.t = httpEx;
            String method =  t.getRequestMethod();
            String headers = "";
            for (Map.Entry<String, List<String>> h : t.getRequestHeaders().entrySet())
            {
                headers += (h + "\n");
            }
            String rHeaders = "";
            for (Map.Entry<String, List<String>> h : t.getResponseHeaders().entrySet())
            {
                rHeaders += (h + "\n");
            }
            // System.out.println("Method: " + method);
            // System.out.println("Headers:\n" + headers);
            // System.out.println("RespHeaders: " + rHeaders);

            if (method.equals("GET"))
            {
                System.out.println(t.getRemoteAddress() + "| GET: " + t.getRequestURI().toString() + " (HanRoot)");
                get(t.getRequestURI().toString());
            }
            else if (method.equals("POST"))
            {
                System.out.println(t.getRemoteAddress() + "| POST: " + t.getRequestURI().toString() + " (HanRoot)");
                post(t.getRequestURI().toString(), null);

            }
            t.close();
        }

        private void get(String uri) throws IOException
        {
            byte[] resp;
            if (uri.matches("/$"))
            {
                resp = getHTML("index.html");
                t.getResponseHeaders().set("content-type", "text/html");
                t.sendResponseHeaders(200, resp.length);
                t.getResponseBody().write(resp);
            }
            else if (uri.matches("/favicon.ico"))
            {
                resp = getFile("favicon.ico");
                t.getResponseHeaders().set("content-type", "attachment");
                t.sendResponseHeaders(200, resp.length);
                t.getResponseBody().write(resp);
            }
            else if (uri.matches("/refresh"))
            {
                resp = "false".getBytes();
                if (getCatStatus())
                {
                    resp = "true".getBytes();
                }
                t.getResponseHeaders().set("content-type", "attachment");
                t.sendResponseHeaders(200, resp.length);
                t.getResponseBody().write(resp);
            }
            else
            {
                resp = resp404.getBytes();
                t.getResponseHeaders().set("content-type", "text/html");
                t.sendResponseHeaders(404, resp.length);
                t.getResponseBody().write(resp);
            }
        }

        private void post(String uri, byte[] payload) throws IOException
        {
            if (uri.matches("/$"))
            {
                //Read form data somehow
                String data = new String(t.getRequestBody().readAllBytes());
                if (!catSpotted && data.contains("name=\"cat\""))
                {
                    int index = data.indexOf("name=\"duration\"");
                    if (index != -1)
                    {
                        System.out.println(data);
                        int i2 = data.indexOf("------", index + 15);
                        data = data.substring(index + 15, i2);
                        data = data.replace("\r", "");
                        data = data.replace("\n", "");
                        try 
                        {
                            time = Integer.parseInt(data) * 1000;
                        }
                        catch (NumberFormatException e)
                        {
                            System.err.println("Unable to parse duration value.");
                        }
                        
                    }
                    setCatStatus(true);
                    new Thread(catReset).start();
                    
                }
                byte[] resp = getHTML("index.html");
                t.getResponseHeaders().set("content-type", "text/html");
                t.sendResponseHeaders(200, resp.length);
                t.getResponseBody().write(resp);
            }
        }
    }

    static class HanRes implements HttpHandler
    {
        HttpExchange t;

        public void handle(HttpExchange httpEx) throws IOException
        {
            this.t = httpEx;
            String method =  t.getRequestMethod();
           
            if (method.equals("GET"))
            {
                System.out.println(t.getRemoteAddress() + "| GET: " + t.getRequestURI().toString() + " (HanRes)");
                get(t.getRequestURI().toString());
            }
            else if (method.equals("POST"))
            {
                System.out.println(t.getRemoteAddress() + "| POST: " + t.getRequestURI().toString() + " (HanRes)");
                byte[] resp = resp404.getBytes();
                t.getResponseHeaders().set("content-type", "text/html");
                t.sendResponseHeaders(404, resp.length);
                t.getResponseBody().write(resp);
            }     
            t.close();       
        }

        private void get(String uri) throws IOException
        {
            byte[] resp = getFile(uri.substring(1));
            if (resp != null) 
            {
                t.getResponseHeaders().set("content-type", "attachment");
                t.sendResponseHeaders(200, resp.length);
                t.getResponseBody().write(resp);
            }
            else
            {
                resp = resp404.getBytes();
                t.getResponseHeaders().set("content-type", "text/html");
                t.sendResponseHeaders(404, resp.length);
                t.getResponseBody().write(resp);
            }
        }
    }

    private static byte[] getHTML(String name) throws IOException
    {
        FileInputStream f = new FileInputStream(name);
        byte[] html = f.readAllBytes();
        f.close();
        return html;
    }

    private static byte[] getFile(String filename)
    {
        byte[] data = null;
        try 
        {
            FileInputStream f = new FileInputStream(filename);
            data = f.readAllBytes();
            f.close();
        }
        catch (IOException e)
        {
            System.out.println("File \"" + filename + "\" cannot be found.");
        }
        return data;
    }

    //this function read and parses a HTTP request from its text format into a HTTPRequest object
    static HTTPRequest parseRequest(InputStream input) throws IOException
    {
        HTTPRequest req = new HTTPRequest();
        
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        
        int b;
        byte last4[] = new byte[4];
        
        while ((b = input.read()) != -1)
            {
            buf.write(b);
            
            last4[0] = last4[1];
            last4[1] = last4[2];
            last4[2] = last4[3];
            last4[3] = (byte) b;
            
            if (last4[0] == '\r' && last4[1] == '\n' && last4[2] == '\r' && last4[3] == '\n')
                {
                break;
                }
            }
        
        if (last4[0] == '\r' && last4[1] == '\n' && last4[2] == '\r' && last4[3] == '\n')
        {
            //format OK
            String data = buf.toString();
            
            String parts[] = data.split("\r\n");
            
            //parse the command line
            String cmd_parts[] = parts[0].split(" ");
            
            if (cmd_parts[0].toLowerCase().equals("get"))
                {
                req.type = RequestType.GET;
                }
                else if (cmd_parts[0].toLowerCase().equals("post"))
                {
                req.type = RequestType.POST;
                }
            
            req.resource = cmd_parts[1];
            
            req.headers = new HTTPHeader[parts.length-1];
            
            //parse the headers
            for (int i = 1; i < parts.length; i++)
            {
                int ndx = parts[i].indexOf(":");
                
                String k = parts[i].substring(0, ndx).trim();
                String v = parts[i].substring(ndx+1).trim();
                
                HTTPHeader nhdr = new HTTPHeader();
                nhdr.key = k.toLowerCase();
                nhdr.value = v;
                
                req.headers[i-1] = nhdr;
            }
        
            return req;
        }
        else
        {
            return null;
        }
    }
}