package com.asierso.ochat.api.models;

import java.util.ArrayList;

public class ModelList {
    private ArrayList<Model> models;

    public ArrayList<Model> getModels() {
        return models;
    }

    public void setModels(ArrayList<Model> models) {
        this.models = models;
    }

    public class Model {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
