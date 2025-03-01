/*
 * Copyright (c) 2023 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.global.extensions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

/**
 * Deep links to the application App Info settings
 * @return `true` if it was able to deep link, otherwise `false`
 */
fun AppCompatActivity.launchApplicationInfoSettings(): Boolean {
    val intent = Intent().apply {
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.parse("package:$packageName")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    runCatching { startActivity(intent) }

    return true
}
