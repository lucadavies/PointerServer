import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

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
            System.out.println("timer sleeping for " + Server.catTime + "ms...");
            Thread.sleep(Server.catTime);
        }
        catch (Exception e)
        {
            System.err.println("Timer interrupted.");
        }
        System.out.println("timer finished.");
        Server.setCatStatus(false);
    }
}

public class Server
{

    static String resp404 = "<html><body><h1>404: Page not found</h3><a href=\"/\">Return to root</a></body></html>";
    static Timer catReset = new Timer(20000);
    static int catTime;
    private static boolean catSpotted = false;
    static String logPath = "server.log";
    static String postPath = "post.txt";
    static LocalDateTime time;
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS");
    static HashMap<String, Integer> postData = new HashMap<String, Integer>();

    public static void main(String[] args) throws Exception 
    {
        System.out.println("Server booting...");
        System.out.println("Loading post data...");
        loadPostData();
        HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
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
        log("setting cat to: " + cat);
        System.out.println("setting cat to: " + cat);
        catSpotted = cat;
    }

    static class HanRoot implements HttpHandler
    {
        HttpExchange t;

        public void handle(HttpExchange httpEx) throws IOException
        {
            this.t = httpEx;
            String method = t.getRequestMethod();
            // String headers = "";
            // for (Map.Entry<String, List<String>> h : t.getRequestHeaders().entrySet())
            // {
            // headers += (h + "\n");
            // }
            // String rHeaders = "";
            // for (Map.Entry<String, List<String>> h : t.getResponseHeaders().entrySet())
            // {
            // rHeaders += (h + "\n");
            // }
            // System.out.println("Method: " + method);
            // System.out.println("Headers:\n" + headers);
            // System.out.println("RespHeaders: " + rHeaders);

            if (method.equals("GET"))
            {
                String s = t.getRemoteAddress() + "| GET: " + t.getRequestURI().toString() + " (HanRoot)";
                log(s);
                System.out.println(s);
                get(t.getRequestURI().toString());
            } else if (method.equals("POST"))
            {
                String s = t.getRemoteAddress() + "| POST: " + t.getRequestURI().toString() + " (HanRoot)";
                log(s);
                System.out.println(s);
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
            else if (uri.matches("/postData"))
            {
                if (postData.size() != 0)
                {
                    String html = "";
                    html += "<table style=\"width:100%\"><tbody>";
                    html += "<tr><th style=\"text-align:left\">Name</th><th style=\"text-align:left\">No.</th><th style=\"text-align:left\"></th></tr>";
                    for (String p : postData.keySet())
                    {
                        html += "<tr>";
                        html += "<td>" + p + "</td>";
                        html += "<td>" + postData.get(p) + "</td>";
                        html += "<td>";
                        html += "<form action=\"/\" method=\"POST\" enctype=\"multipart/form-data\">";
                        html += "<button type=\"submit\" name=\"remPost\" value=\"" + p + "\" >Collect</button>";
                        html += "</form>";
                        html += "</td>";
                        html += "</tr>";
                    }
                    html += "</tbody></table>";
                    resp = html.getBytes();
                }
                else
                {
                    resp = "<p>No post reported yet</p>".getBytes();
                }
                
                t.getResponseHeaders().set("content-type", "text/plain");
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
                // hate this but Java 8 affords not many better ways
                String data = new BufferedReader(new InputStreamReader(t.getRequestBody())).lines().collect(Collectors.joining("\n"));
                HashMap<String, String> form = parseForm(data);
                if (form.keySet().contains("cat"))
                {
                    catTime = 20000;
                    if (form.keySet().contains("duration"))
                    {
                        try 
                        {
                            catTime = Integer.parseInt(form.get("duration")) * 1000;
                        }
                        catch (NumberFormatException e)
                        {
                            System.err.println("Unable to parse duration value.");
                        }
                    }
                    setCatStatus(true);
                    new Thread(catReset).start();
                }
                else if (form.keySet().contains("repPost")) //report post
                {
                    String name = form.get("name");
                    if (!postData.keySet().contains(name)) //if not in post data set
                    {
                        postData.put(name, 1);
                    }
                    else
                    {
                        postData.put(name, postData.get(name) + 1); //else add one to current value
                    }
                    updatePostLog();
                }
                else if (form.keySet().contains("remPost")) //collect post
                {
                    postData.remove(form.get("remPost"));
                    updatePostLog();
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
                String s = t.getRemoteAddress() + "| GET: " + t.getRequestURI().toString() + " (HanRes)";
                log(s);
                System.out.println(s);
                get(t.getRequestURI().toString());
            }
            else if (method.equals("POST"))
            {
                String s = t.getRemoteAddress() + "| POST: " + t.getRequestURI().toString() + " (HanRes)";
                log(s);
                System.out.println(s);
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
        byte[] html = Files.readAllBytes(new File(name).toPath());
        return html;
    }

    private static byte[] getFile(String filename)
    {
        byte[] data = null;
        try 
        {
            data = Files.readAllBytes(new File(filename).toPath());
        }
        catch (IOException e)
        {
            System.out.println("File \"" + filename + "\" cannot be found.");
        }
        return data;
    }

    private static HashMap<String, String> parseForm(String data)
    {
        HashMap<String, String> form = new HashMap<String, String>();
        int index = 0;
        int prevIndex;
        while (true)
        {
            prevIndex = index;
            index = data.indexOf("form-data; name=\"", index) + 17; //start of field name
            if (index != -1 && index > prevIndex)
            {
                int start = index;
                int end = data.indexOf("\"", start);                //end of field name
                String key = data.substring(start, end);
                start = end + 3;                             //end of field name: start of data (less leading \n\n)
                end = data.indexOf("------WebKit", end) - 1;    //end of data (less trailing \n);
                String value = data.substring(start, end);
                form.put(key, value);
                index = end;
            }
            else
            {
                break;
            }
        }
        return form;
    }

    static void loadPostData()
    {
        try
        {
            Scanner scanner = new Scanner(new File(postPath));
            while (scanner.hasNextLine())
            {
                String[] dat = scanner.nextLine().split(",");
                postData.put(dat[0], Integer.parseInt(dat[1]));
            }
        }
        catch (IOException e)
        {
            System.out.println("Error read post log.");
        }
        
    }

    static void log(String s)
    {
        time = LocalDateTime.now();
        try
        {
            PrintWriter l = new PrintWriter(new FileOutputStream(new File(logPath), true));
            l.append("[" + time.format(formatter) + "]: " + s + "\n");
            l.close();
        }
        catch (IOException e)
        {
            System.out.println("Error writing to log.");
            System.out.println("Failed to log: \"" + s + "\"");
        }
        
    }

    static void updatePostLog()
    {
        try
        {
            PrintWriter l = new PrintWriter(new FileOutputStream(new File(postPath), false));
            for (String s : postData.keySet())
            {
                l.write(s + "," + postData.get(s) + "\n");
            }
            l.close();
        }
        catch (IOException e)
        {
            System.out.println("Error writing to post log.");
        }
    }
}
