package id.ac.ui.cs.advprog.yomu.auth.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.springframework.stereotype.Component;

@Component
public class DnsEmailExistenceChecker implements EmailExistenceChecker {

    @Override
    public boolean exists(String email) {
        String domain = extractDomain(email);
        if (domain.isBlank()) {
            return false;
        }

        return hasMxRecord(domain) || hasHostRecord(domain);
    }

    private String extractDomain(String email) {
        if (email == null) {
            return "";
        }
        int atIndex = email.lastIndexOf('@');
        if (atIndex < 0 || atIndex == email.length() - 1) {
            return "";
        }
        return email.substring(atIndex + 1).trim().toLowerCase();
    }

    private boolean hasMxRecord(String domain) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

        DirContext context = null;
        try {
            context = new InitialDirContext(env);
            Attributes attributes = context.getAttributes(domain, new String[]{"MX"});
            return attributes.get("MX") != null && attributes.get("MX").size() > 0;
        } catch (NamingException exception) {
            return false;
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException ignored) {
                    // Best-effort cleanup only.
                }
            }
        }
    }

    private boolean hasHostRecord(String domain) {
        try {
            InetAddress.getByName(domain);
            return true;
        } catch (UnknownHostException exception) {
            return false;
        }
    }
}
