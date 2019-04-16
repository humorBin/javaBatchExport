/**
 * 批量导出数据
 */
function batchExport() {
    var getSelectRows = $("#ReportTable").bootstrapTable('getSelections', function (row) { //获取页面选中的对象
        return row;
    });
    var reportIdArr = [];
    if (getSelectRows.length !=0) {
        for (var i = 0; i < getSelectRows.length; i++) {
            reportIdArr.push(getSelectRows[i].id);
        }
        // alert(reportIdArr.join(','));
        window.open('/report/batchExport?reportArr=' + reportIdArr.join(','));//访问后台同时传参
    }else{
        layer.msg("请选择要导出的数据");
    }
}
