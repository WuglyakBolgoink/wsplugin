package ws;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NonNls;
import ws.index.WSFileBasedIndexExtension;

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WSUtil {

    protected static String REGEX_PATTERN = "(\\w+!)?(SBIS3\\.\\w+\\.\\w+)(.*)?";
    protected static String REGEX_PATTERN_WITH_PREFIX = "^['\"]js!(SBIS3\\.\\w+\\.\\w+)(.*)";

    public static String[] parseComponentName(String text) {
        String[] result = {"", "", null, ""};
        text = text.replace("IntellijIdeaRulezzz", ""); //fixme: IntellijIdeaRulezzz

        Pattern pattern = Pattern.compile(REGEX_PATTERN);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            result[0] = matcher.group(2);
            result[1] = matcher.group(3) != null ? matcher.group(3) : "";
            result[3] = matcher.group(1) != null ? matcher.group(1).replace("!", "") : "";

            String[] str = result[1].split(":");
            if (str.length > 1) {
                result[1] = str[0];
                result[2] = str[1];
            }
        }

        return result;
    }

    public static Collection<String> getAllChildFiles(VirtualFile file, String key, String type) {
        Collection<String> result = new HashSet<String>();
        String fileName = file.getName();
        String filePath = file.getPath();
        String fileShortPath = filePath.replace(fileName, "");

        VirtualFile[] childFiles = file.isDirectory() ? file.getChildren() : file.getParent().getChildren();
        for (VirtualFile childFile : childFiles) {
            String path = childFile.getPath();
            path = path.replace(fileShortPath, "");
            if (childFile.isDirectory()) {
                result.addAll(getChildFiles(childFile, key));
            } else if (!path.isEmpty() && !path.equals(fileName) && (type.isEmpty() || path.endsWith("." + type)) && !path.endsWith(".module.js")) {
                result.add(key + "/" + (type.isEmpty() ? path : path.replace("." + type, "")));
            }
        }
        return result;
    }

    public static Collection<String> getChildFiles(VirtualFile file, String key) {
        return getAllChildFiles(file, key, "js");
    }

    @NonNls
    public static Collection<String> getVariantsByName(@NonNls String[] parseResult, Project project) {

        Collection<String> result = new HashSet<String>();
        String componentName = parseResult[0];
        String postKey = parseResult[1];

        if (!componentName.isEmpty()) {
            Collection<VirtualFile> files = WSFileBasedIndexExtension.getFileByComponentName(project, componentName);
            for (VirtualFile file : files) {
                if (!postKey.isEmpty()) {
                    result.addAll(WSUtil.getChildFiles(file, componentName));
                }
            }
        } else {
            result = WSFileBasedIndexExtension.getAllComponentNames(project);
        }

        return result;
    }

    public static Collection<PsiElement> getFilesByNameWithPrefix(@NonNls String[] parseResult, Project project) {
        String pref = parseResult[3];
        Collection<PsiElement> resultFilesCollection = new HashSet<PsiElement>();

        String controlName = parseResult[0];
        String path = parseResult[1];

        if (pref == null || !pref.matches("^(css|js|html)$") || controlName == null || controlName.isEmpty()) {
            return resultFilesCollection;
        }

        if(pref.equals("html")){
            pref = "xhtml";
        }

        Collection<VirtualFile> files = WSFileBasedIndexExtension.getFileByComponentName(project, controlName);

        if (files.size() == 0) {
            return resultFilesCollection;
        }

        final PsiManager instance = PsiManager.getInstance(project);

        for (VirtualFile file : files) {
            if (path != null && !path.isEmpty()) {
                VirtualFile pathFile = file.getParent().findFileByRelativePath(path + "." + pref);
                if (pathFile != null) {
                    file = pathFile;
                } else {
                    continue;
                }
            } else if (!pref.equals("js")) {
                VirtualFile pathFile = file.getParent().findFileByRelativePath(file.getName().replace("module.js","") + pref);
                if (pathFile != null) {
                    file = pathFile;
                } else {
                    continue;
                }
            }
            try {
                PsiFile psiFile = instance.findFile(file);
                if (psiFile != null) {
                    resultFilesCollection.add(instance.findFile(file));
                }
            } catch (Exception ignore) {
            }
        }


        return resultFilesCollection;
    }
}
