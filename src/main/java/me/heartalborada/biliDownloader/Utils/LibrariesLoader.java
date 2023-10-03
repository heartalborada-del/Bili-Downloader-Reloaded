package me.heartalborada.biliDownloader.Utils;

import me.heartalborada.biliDownloader.Main;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.logging.Logger;

import static me.heartalborada.biliDownloader.Utils.LoggerFormatter.installFormatter;

/**
 * Jar 库加载器
 */
public class LibrariesLoader {
    private static final Logger logger = installFormatter(Logger.getLogger("Dependency Loader"));

    private static final LinkedList<String> list = new LinkedList<>();
    private static void downloadFile(File file, URL url) throws IOException {
        try (InputStream is = url.openStream()) {
            Files.copy(is, file.toPath());
        }
    }

    /**
     * 从Maven仓库下载依赖
     *
     * @param groupId    组ID
     * @param artifactId 构建ID
     * @param version    版本
     * @param repo       仓库地址
     * @param extra      额外参数
     * @param file       保存文件
     * @param checkMD5   是否检查MD5
     * @return 下载成功返回true，否则返回false
     */
    static boolean downloadLibraryMaven(String groupId, String artifactId, String version, String extra, String repo, File file, boolean checkMD5) throws RuntimeException, IOException, NoSuchAlgorithmException {
        // 创建文件夹
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs())
            throw new RuntimeException("Failed to create " + file.getParentFile().getPath());

        // 下载地址格式
        if (!repo.endsWith("/")) repo += "/";
        repo += "%s/%s/%s/%s-%s%s.jar";
        String DownloadURL = String.format(repo, groupId.replace(".", "/"), artifactId, version, artifactId, version, extra); // 下载地址
        String FileName = artifactId + "-" + version + ".jar"; // 文件名

        // 检查MD5
        if (checkMD5) {
            File FileMD5 = new File(file.getParentFile(), FileName + ".md5");
            String DownloadMD5Url = DownloadURL + ".md5";
            URL DownloadMD5UrlFormat = new URL(DownloadMD5Url);

            if (FileMD5.exists() && !FileMD5.delete())
                throw new RuntimeException("Failed to delete " + FileMD5.getPath());

            downloadFile(FileMD5, DownloadMD5UrlFormat); // 下载MD5文件

            if (!FileMD5.exists()) throw new RuntimeException("Failed to download " + DownloadMD5Url);

            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                boolean isSame = getInputStreamMD5(fis).equals(Files.readString(FileMD5.toPath()));
                if (!isSame) {
                    fis.close();
                    if (!file.delete()) throw new RuntimeException("Failed to delete " + file.getPath());
                }
            }
        } else if (file.exists() && !file.delete()) { // 不检查直接删原文件下新的
            throw new RuntimeException("Failed to delete " + file.getPath());
        }

        // 下载正式文件
        if (!file.exists()) {
            logger.info("Downloading " + DownloadURL);
            downloadFile(file, new URL(DownloadURL));
        }

        return file.exists();
    }

    /**
     * 从Maven仓库下载Pom
     *
     * @param groupId    组ID
     * @param artifactId 构建ID
     * @param version    版本
     * @param repo       仓库地址
     * @param extra      额外参数
     * @param file       保存文件
     * @param checkMD5   是否检查MD5
     * @return 下载成功返回true，否则返回false
     */
    static boolean downloadLibraryPomMaven(String groupId, String artifactId, String version, String extra, String repo, File file, boolean checkMD5) throws RuntimeException, IOException, NoSuchAlgorithmException {
        // 创建文件夹
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs())
            throw new RuntimeException("Failed to create " + file.getParentFile().getPath());

        // 下载地址格式
        if (!repo.endsWith("/")) repo += "/";
        repo += "%s/%s/%s/%s-%s%s.pom";
        String DownloadURL = String.format(repo, groupId.replace(".", "/"), artifactId, version, artifactId, version, extra); // 下载地址
        String FileName = artifactId + "-" + version + ".pom"; // 文件名

        if (checkMD5) {
            File FileMD5 = new File(file.getParentFile(), FileName + ".md5");
            String DownloadMD5Url = DownloadURL + ".md5";
            URL DownloadMD5UrlFormat = new URL(DownloadMD5Url);

            if (FileMD5.exists() && !FileMD5.delete())
                throw new RuntimeException("Failed to delete " + FileMD5.getPath());

            downloadFile(FileMD5, DownloadMD5UrlFormat); // 下载MD5文件

            if (!FileMD5.exists()) throw new RuntimeException("Failed to download " + DownloadMD5Url);

            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                boolean isSame = getInputStreamMD5(fis).equals(Files.readString(FileMD5.toPath()));
                if (!isSame) {
                    fis.close();
                    if (!file.delete()) throw new RuntimeException("Failed to delete " + file.getPath());
                }
            }
        } else if (file.exists() && !file.delete()) { // 不检查直接删原文件下新的
            throw new RuntimeException("Failed to delete " + file.getPath());
        }

        // 下载正式文件
        if (!file.exists()) {
            logger.info("Downloading " + DownloadURL);
            downloadFile(file, new URL(DownloadURL));
        }

        return file.exists();
    }

    /**
     * 获取依赖Maven仓库最新版本
     *
     * @param groupId    组ID
     * @param artifactId 构件ID
     * @param repoUrl    仓库地址
     * @param xmlTag     XML标签
     * @return 版本名
     */
    public static String getLibraryVersionMaven(String groupId, String artifactId, String repoUrl, String xmlTag) throws RuntimeException, IOException, ParserConfigurationException, SAXException, NoSuchAlgorithmException {
        File CacheDir = new File(Main.getDataPath(), "cache");
        if (!CacheDir.exists() && !CacheDir.mkdirs())
            throw new RuntimeException("Failed to create " + CacheDir.getPath());
        String metaFileName = "maven-metadata-" + groupId + "." + artifactId + ".xml";
        File metaFile = new File(CacheDir, metaFileName);

        if (!repoUrl.endsWith("/")) repoUrl += "/";
        repoUrl += "%s/%s/"; // 根目录格式
        String repoFormat = String.format(repoUrl, groupId.replace(".", "/"), artifactId); // 格式化后的根目录

        // MD5
        File metaFileMD5 = new File(CacheDir, metaFileName + ".md5");
        if (metaFileMD5.exists() && !metaFileMD5.delete())
            throw new RuntimeException("Failed to delete " + metaFileMD5.getPath());

        URL metaFileMD5Url = new URL(repoFormat + "maven-metadata.xml.md5");

        downloadFile(metaFileMD5, metaFileMD5Url);

        if (!metaFileMD5.exists()) throw new RuntimeException("Failed to download " + metaFileMD5Url);

        // 验证meta文件
        logger.finest("Verifying " + metaFileName);
        if (metaFile.exists()) {
            try (FileInputStream fis = new FileInputStream(metaFile)) {
                if (!getInputStreamMD5(fis).equals(Files.readString(metaFileMD5.toPath()))) {
                    fis.close();
                    if (!metaFile.delete()) throw new RuntimeException("Failed to delete " + metaFile.getPath());

                    URL metaFileUrl = new URL(repoFormat + "maven-metadata.xml");
                    downloadFile(metaFile, metaFileUrl);
                    if (!metaFileMD5.exists()) throw new RuntimeException("Failed to download " + metaFileUrl);
                }
            }
        } else {
            URL metaFileUrl = new URL(repoFormat + "maven-metadata.xml");
            logger.info("Downloading " + metaFileUrl);
            downloadFile(metaFile, metaFileUrl);
            if (!metaFileMD5.exists()) throw new RuntimeException("Failed to download " + metaFileUrl);
        }

        // 读取内容
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(metaFile);
        return doc.getElementsByTagName(xmlTag).item(0).getFirstChild().getNodeValue();
    }

    /**
     * 加载 Maven 仓库的依赖库
     *
     * @param groupId    组ID
     * @param artifactId 构件ID
     * @param version    版本
     * @param repo       仓库地址
     * @param extra      额外参数
     * @param path       保存目录
     */
    public static void loadLibraryClassMaven(String groupId, String artifactId, String version, String extra, String repo, File path) throws RuntimeException, IOException, NoSuchAlgorithmException, ParserConfigurationException, SAXException {

        String target = String.format("%s-%s-%s%s",groupId,artifactId,version,extra);
        if(list.contains(target)) {
            return;
        }
        list.add(target);
        loadLibraryLibrary(groupId, artifactId, version, extra,repo, path);
        String name = artifactId + "-" + version + ".jar"; // 文件名
        // jar
        File saveLocation = new File(path, String.format("%s/%s/%s/%s",groupId.replace(".","/"),artifactId,version,name));
        logger.finest("Verifying " + name);
        if (!downloadLibraryMaven(groupId, artifactId, version, extra, repo, saveLocation, true)) {
            throw new RuntimeException("Failed to download libraries!");
        }
        // -- 加载开始 --
        loadLibraryClassLocal(saveLocation);
    }

    /**
     * 获取依赖的依赖并加载
     *
     * @param groupId    组ID
     * @param artifactId 构件ID
     * @param version    版本
     * @param extra      额外参数
     */
    public static void loadLibraryLibrary(String groupId, String artifactId, String version, String extra,String repo, File path) throws RuntimeException, IOException, NoSuchAlgorithmException, ParserConfigurationException, SAXException {
        String name = artifactId + "-" + version + ".pom"; // 文件名

        // jar
        File saveLocation = new File(path, String.format("%s/%s/%s/%s",groupId.replace(".","/"),artifactId,version,name));

        logger.finest("Verifying " + name);
        if (!downloadLibraryPomMaven(groupId, artifactId, version, extra, repo, saveLocation,true)) {
            throw new RuntimeException("Failed to download library's pom!");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(saveLocation);
        NodeList list = doc.getElementsByTagName("dependency");
        for (int i = 0; i <list.getLength() ; i++) {
            Node node = list.item(i);
            NodeList childNodes = node.getChildNodes();
            String gId = "",aId = "",ver="",scope="";
            for (int j = 0; j <childNodes.getLength() ; j++) {
                if (childNodes.item(j).getNodeType()==Node.ELEMENT_NODE) {
                    switch (childNodes.item(j).getNodeName()) {
                        case "groupId":
                            gId = childNodes.item(j).getFirstChild().getNodeValue();
                            break;
                        case "artifactId":
                            aId = childNodes.item(j).getFirstChild().getNodeValue();
                            break;
                        case "scope":
                            scope = childNodes.item(j).getFirstChild().getNodeValue();
                            break;
                        case "version":
                            ver = childNodes.item(j).getFirstChild().getNodeValue();
                            break;
                    }
                }
            }
            if(gId.equals("")||aId.equals("")||ver.equals("")||!scope.equals("compile")) {
                break;
            }
            loadLibraryClassMaven(gId, aId, ver, extra, repo, path);
        }
    }

    /**
     * 加载本地 Jar
     *
     * @param file Jar 文件
     */
    static void loadLibraryClassLocal(File file) throws IOException {
        logger.info("Loading library " + file.getName());
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = file.toURI().toURL();
        if (classLoader instanceof URLClassLoader) {
            //Java 8
            URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Class<URLClassLoader> sysClass = URLClassLoader.class;
            try {
                Method method = sysClass.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(sysLoader, url);
            } catch (Exception var5) {
                var5.printStackTrace();
                throw new IllegalStateException(var5.getMessage(), var5);
            }
        } else {
            try {
                Field field;
                try {
                    // Java 9 - 15
                    field = classLoader.getClass().getDeclaredField("ucp");
                } catch (NoSuchFieldException e) {
                    // Java 16+
                    field = classLoader.getClass().getSuperclass().getDeclaredField("ucp");
                }
                field.setAccessible(true);
                Object ucp = field.get(classLoader);
                Method method = ucp.getClass().getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(ucp, url);
            } catch (Exception exception) {
                exception.printStackTrace();
                throw new IllegalStateException(exception.getMessage(), exception);
            }
        }
    }

    private static String getInputStreamMD5(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");

        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        while ((bytesCount = inputStream.read(byteArray)) != -1)
        {
            digest.update(byteArray, 0, bytesCount);
        }

        inputStream.close();

        byte[] bytes = digest.digest();

        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
}