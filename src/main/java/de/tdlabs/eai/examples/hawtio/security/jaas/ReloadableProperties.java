package de.tdlabs.eai.examples.hawtio.security.jaas;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReloadableProperties {
    private static final Logger LOG = LoggerFactory.getLogger(ReloadableProperties.class);

    private Properties props = new Properties();

    private Map<String, String> invertedProps;

    private Map<String, Set<String>> invertedValueProps;

    private long reloadTime = -1;

    private final PropertiesLoader.FileNameKey key;

    public ReloadableProperties(PropertiesLoader.FileNameKey key) {
        this.key = key;
    }

    public synchronized Properties getProps() {
        return props;
    }

    public synchronized ReloadableProperties obtained() {
        if (reloadTime < 0 || (key.isReload() && hasModificationAfter(reloadTime))) {
            props = new Properties();
            try {
                load(key.file(), props);
                invertedProps = null;
                invertedValueProps = null;
                if (key.isDebug()) {
                    LOG.debug("Load of: " + key);
                }
            } catch (IOException e) {
                LOG.error("Failed to load: " + key + ", reason:" + e.getLocalizedMessage());
                if (key.isDebug()) {
                    LOG.debug("Load of: " + key + ", failure exception" + e);
                }
            }
            reloadTime = System.currentTimeMillis();
        }
        return this;
    }

    public synchronized Map<String, String> invertedPropertiesMap() {
        if (invertedProps == null) {
            invertedProps = new HashMap<>(props.size());
            for (Map.Entry<Object, Object> val : props.entrySet()) {
                invertedProps.put((String) val.getValue(), (String) val.getKey());
            }
        }
        return invertedProps;
    }

    public synchronized Map<String, Set<String>> invertedPropertiesValuesMap() {
        if (invertedValueProps == null) {
            invertedValueProps = new HashMap<>(props.size());
            for (Map.Entry<Object, Object> val : props.entrySet()) {
                String[] userList = ((String) val.getValue()).split(",");
                for (String user : userList) {
                    Set<String> set = invertedValueProps.get(user);
                    if (set == null) {
                        set = new HashSet<>();
                        invertedValueProps.put(user, set);
                    }
                    set.add((String) val.getKey());
                }
            }
        }
        return invertedValueProps;
    }

    private void load(final File source, Properties props) throws IOException {
        FileInputStream in = new FileInputStream(source);
        try {
            props.load(in);
//            if (key.isDecrypt()) {
//                try {
//                    EncryptionSupport.decrypt(this.props);
//                } catch (NoClassDefFoundError e) {
//                    // this Happens whe jasypt is not on the classpath..
//                    key.setDecrypt(false);
//                    LOG.info("jasypt is not on the classpath: password decryption disabled.");
//                }
//            }

        } finally {
            in.close();
        }
    }

    private boolean hasModificationAfter(long reloadTime) {
        return key.file.lastModified() > reloadTime;
    }

}
