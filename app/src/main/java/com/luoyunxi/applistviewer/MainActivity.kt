package com.luoyunxi.applistviewer

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.PackageInfoCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.promeg.pinyinhelper.Pinyin


class MainActivity : AppCompatActivity() {

    private lateinit var spinner: Spinner
    private lateinit var checkHideNoLauncher: CheckBox
    private lateinit var checkHideSystem: CheckBox
    private lateinit var btnQuery: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var editSearch: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter

    private var allApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spinner = findViewById(R.id.queryMethodSpinner)
        checkHideNoLauncher = findViewById(R.id.checkHideNoLauncher)
        checkHideSystem = findViewById(R.id.checkHideSystem)
        btnQuery = findViewById(R.id.btnQuery)
        progressBar = findViewById(R.id.progressBar)
        editSearch = findViewById(R.id.editSearch)
        recyclerView = findViewById(R.id.recyclerView)

        adapter = AppListAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnQuery.setOnClickListener {
            loadApps()
        }

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterList(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }





    private fun loadApps() {
        adapter.submitList(emptyList())
        progressBar.visibility = View.VISIBLE
        btnQuery.isEnabled = false

        Thread {
            val pm = packageManager
            val hideNoLauncher = checkHideNoLauncher.isChecked
            val hideSystem = checkHideSystem.isChecked
            val useInstalledApplications = spinner.selectedItemPosition == 0

            val rawList = if (useInstalledApplications) {
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            } else {
                val intent = Intent(Intent.ACTION_MAIN)
                pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
                    .map { it.activityInfo.applicationInfo }
                    .distinctBy { it.packageName }
            }

            val filtered = rawList.filter { app ->
                val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isLauncherApp = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                    .let { intent ->
                        pm.queryIntentActivities(intent, 0)
                            .any { it.activityInfo.applicationInfo.packageName == app.packageName }
                    }

                (!hideSystem || !isSystemApp) && (!hideNoLauncher || isLauncherApp)
            }


            val appList = filtered.map {
                val currentPackageName = it.packageName
                val appLabel = pm.getApplicationLabel(it).toString()

                val versionString: String = try {
                    val packageInfo: PackageInfo = pm.getPackageInfo(currentPackageName, 0)
                    val versionName: String = packageInfo.versionName ?: "N/A"
                    val versionCode: Long = PackageInfoCompat.getLongVersionCode(packageInfo)
                    "$versionName ($versionCode)" // Format the string
                } catch (e: PackageManager.NameNotFoundException) {
                    "N/A (N/A)"
                } catch (e: Exception) {
                    "Error (Error)"
                }
                val infoBuilder = StringBuilder()

                try {
                    if (it.metaData?.containsKey("xposedminversion") == true) {
                        infoBuilder.append("[Xposed]")
                    }
                } catch (e: Exception) {
                    if (infoBuilder.isNotEmpty()) infoBuilder.append(" ")
                    infoBuilder.append("[元数据错误]")
                }

                val targetAPI = it.targetSdkVersion
                val targetApiString = " (Target API: $targetAPI)"


                if (targetAPI < Build.VERSION_CODES.R) {
                    infoBuilder.append("无需声明权限即可获取应用信息")
                } else {
                    try {
                        val packageInfoWithPerms: PackageInfo = pm.getPackageInfo(
                            currentPackageName,
                            PackageManager.GET_PERMISSIONS
                        )
                        val permissions: Array<String>? = packageInfoWithPerms.requestedPermissions

                        val hasQueryPermission = permissions?.contains(Manifest.permission.QUERY_ALL_PACKAGES) == true

                        if (hasQueryPermission) {
                            infoBuilder.append("已声明查询应用权限")
                        } else {
                            infoBuilder.append(" 未声明查询应用权限")
                        }
                    } catch (e: PackageManager.NameNotFoundException) {
                        infoBuilder.append(" 权限检查失败-未找到")
                    } catch (e: Exception) {
                        infoBuilder.append(" 权限检查出错")
                    }
                }
                infoBuilder.append(targetApiString)


                AppInfo(
                    appName = appLabel,
                    packageName = currentPackageName,
                    moreInfo = infoBuilder.toString().trim(),
                    appVersion = versionString,
                )
            }.sortedBy { appInfo ->
                // 将整个名称转换为拼音（非中文字符保持不变），然后转小写排序
                Pinyin.toPinyin(appInfo.appName, "").lowercase()
            }//.sortedBy { it.appName.lowercase() }

            runOnUiThread {
                allApps = appList
                filterList(editSearch.text.toString())
                progressBar.visibility = View.GONE
                btnQuery.isEnabled = true
            }
        }.start()
    }


    private fun filterList(keyword: String) {
        val lower = keyword.trim().lowercase()
        val result = if (lower.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.appName.lowercase().contains(lower) || it.packageName.lowercase().contains(lower)
            }
        }
        adapter.submitList(result)
    }

}