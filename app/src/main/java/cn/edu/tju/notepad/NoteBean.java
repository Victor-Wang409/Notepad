package cn.edu.tju.notepad;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NoteBean implements Serializable {
    private int id;
    private String title;
    private String content;
    private String time;
    private List<String> imagePaths; // 存储图片路径的列表

    public NoteBean(String title, String content, String time) {
        this.title = title;
        this.content = content;
        this.time = time;
        this.imagePaths = new ArrayList<>();
    }

    public NoteBean(int id, String title, String content, String time) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.time = time;
        this.imagePaths = new ArrayList<>();
    }

    public NoteBean(int id, String title, String content, String time, List<String> imagePaths) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.time = time;
        this.imagePaths = imagePaths != null ? imagePaths : new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<String> getImagePaths() {
        return imagePaths;
    }

    public void setImagePaths(List<String> imagePaths) {
        this.imagePaths = imagePaths;
    }

    public void addImagePath(String imagePath) {
        if (this.imagePaths == null) {
            this.imagePaths = new ArrayList<>();
        }
        this.imagePaths.add(imagePath);
    }

    // 用于将图片路径列表转换为存储用的字符串
    public String getImagePathsAsString() {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < imagePaths.size(); i++) {
            sb.append(imagePaths.get(i));
            if (i < imagePaths.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    // 用于从存储的字符串恢复图片路径列表
    public static List<String> parseImagePathsFromString(String pathsString) {
        List<String> paths = new ArrayList<>();
        if (pathsString != null && !pathsString.isEmpty()) {
            String[] pathArray = pathsString.split(";");
            for (String path : pathArray) {
                if (!path.trim().isEmpty()) {
                    paths.add(path.trim());
                }
            }
        }
        return paths;
    }
}