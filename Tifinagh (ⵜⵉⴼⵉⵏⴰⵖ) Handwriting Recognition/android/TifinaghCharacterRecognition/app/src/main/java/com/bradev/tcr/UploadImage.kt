package com.bradev.tcr

import com.android.volley.Response
import com.android.volley.toolbox.StringRequest

class UploadImage(
        method: Int, url: String?, listener: Response.Listener<String>?,
        errorListener: Response.ErrorListener?) : StringRequest(method, url, listener, errorListener
) {

    var data: String = ""
    var params: HashMap<String, String>? = null

    override fun getParams(): MutableMap<String, String> {
        params = HashMap()
        params!!["data"] = data
        return params!!
    }

}