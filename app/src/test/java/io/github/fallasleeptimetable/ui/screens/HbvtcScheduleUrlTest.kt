package io.github.fallasleeptimetable.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HbvtcScheduleUrlTest {
    @Test
    fun opensSemesterScheduleOnTheCurrentOrigin() {
        assertEquals(
            "http://jwgl.hbvtc.edu.cn/jsxsd/xskb/xskb_list.do",
            hbvtcScheduleUrl("http://jwgl.hbvtc.edu.cn/jsxsd/framework/xsMainV.htmlx"),
        )
        assertEquals(
            "https://jwgl.hbvtc.edu.cn:8443/jsxsd/xskb/xskb_list.do",
            hbvtcScheduleUrl("https://jwgl.hbvtc.edu.cn:8443/jsxsd/framework/xsMainV_new.htmlx?t1=1#home"),
        )
    }

    @Test
    fun leavesTheShortcutHiddenOutsideTheStudentDashboard() {
        val unrelatedPages = listOf(
            null,
            "https://casp.hbvtc.edu.cn/#/",
            "http://jwgl.hbvtc.edu.cn/jsxsd/xskb/xskb_list.do",
            "https://jwgl.hbvtc.edu.cn.example.com/jsxsd/framework/xsMainV.htmlx",
            "ftp://jwgl.hbvtc.edu.cn/jsxsd/framework/xsMainV.htmlx",
            "https://user@jwgl.hbvtc.edu.cn/jsxsd/framework/xsMainV.htmlx",
            "invalid URL",
        )
        unrelatedPages.forEach { assertNull(it, hbvtcScheduleUrl(it)) }
    }
}
