package io.github.gohoski.numai;

import java.io.InputStream;

/**
 * Created by Gleb on 11.10.2025.
 * class to get the selected model + InputStream
 * it is not recommended to display the model from the response json since
 * it usually contains unnecessary prefixes, which is why this exists !
 */

class ApiResult {
    private final String model;
    private final InputStream result;

    ApiResult(String model, InputStream result) {
        this.model = model;
        this.result = result;
    }

    String getModel() {
        return model;
    }

    InputStream getResult() {
        return result;
    }
}