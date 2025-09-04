/**
 * Copyright 2021 json.cn
 */
package com.taobao.logistics.entity;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

public class PrintView {

    private String cmd;
    private String requestID;
    private String version;
    private Task task;
    public void setCmd(String cmd) {
        this.cmd = cmd;
    }
    public String getCmd() {
        return cmd;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }
    public String getRequestID() {
        return requestID;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    public String getVersion() {
        return version;
    }

    public void setTask(Task task) {
        this.task = task;
    }
    public Task getTask() {
        return task;
    }




    public static class Task {

        private String taskID;
        private boolean preview;
        private String printer;
        private String previewType;
        private List<Documents> documents;
        public void setTaskID(String taskID) {
            this.taskID = taskID;
        }
        public String getTaskID() {
            return taskID;
        }

        public void setPreview(boolean preview) {
            this.preview = preview;
        }
        public boolean getPreview() {
            return preview;
        }

        public void setPrinter(String printer) {
            this.printer = printer;
        }
        public String getPrinter() {
            return printer;
        }

        public void setPreviewType(String previewType) {
            this.previewType = previewType;
        }
        public String getPreviewType() {
            return previewType;
        }

        public void setDocuments(List<Documents> documents) {
            this.documents = documents;
        }
        public List<Documents> getDocuments() {
            return documents;
        }

    }



    public static class Documents {

        private String documentID;
        private List<Contents> contents;
        public void setDocumentID(String documentID) {
            this.documentID = documentID;
        }
        public String getDocumentID() {
            return documentID;
        }

        public void setContents(List<Contents> contents) {
            this.contents = contents;
        }
        public List<Contents> getContents() {
            return contents;
        }

    }



    public static class Contents {

        private JSONObject data;
        private String templateURL;
        public void setData(JSONObject data) {
            this.data = data;
        }
        public JSONObject getData() {
            return data;
        }

        public void setTemplateURL(String templateURL) {
            this.templateURL = templateURL;
        }
        public String getTemplateURL() {
            return templateURL;
        }

    }
}