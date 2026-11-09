/*
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
var showControllersOnly = false;
var seriesFilter = "";
var filtersOnlySampleSeries = true;

/*
 * Add header in statistics table to group metrics by category
 * format
 *
 */
function summaryTableHeader(header) {
    var newRow = header.insertRow(-1);
    newRow.className = "tablesorter-no-sort";
    var cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Requests";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 3;
    cell.innerHTML = "Executions";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 7;
    cell.innerHTML = "Response Times (ms)";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Throughput";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 2;
    cell.innerHTML = "Network (KB/sec)";
    newRow.appendChild(cell);
}

/*
 * Populates the table identified by id parameter with the specified data and
 * format
 *
 */
function createTable(table, info, formatter, defaultSorts, seriesIndex, headerCreator) {
    var tableRef = table[0];

    // Create header and populate it with data.titles array
    var header = tableRef.createTHead();

    // Call callback is available
    if(headerCreator) {
        headerCreator(header);
    }

    var newRow = header.insertRow(-1);
    for (var index = 0; index < info.titles.length; index++) {
        var cell = document.createElement('th');
        cell.innerHTML = info.titles[index];
        newRow.appendChild(cell);
    }

    var tBody;

    // Create overall body if defined
    if(info.overall){
        tBody = document.createElement('tbody');
        tBody.className = "tablesorter-no-sort";
        tableRef.appendChild(tBody);
        var newRow = tBody.insertRow(-1);
        var data = info.overall.data;
        for(var index=0;index < data.length; index++){
            var cell = newRow.insertCell(-1);
            cell.innerHTML = formatter ? formatter(index, data[index]): data[index];
        }
    }

    // Create regular body
    tBody = document.createElement('tbody');
    tableRef.appendChild(tBody);

    var regexp;
    if(seriesFilter) {
        regexp = new RegExp(seriesFilter, 'i');
    }
    // Populate body with data.items array
    for(var index=0; index < info.items.length; index++){
        var item = info.items[index];
        if((!regexp || filtersOnlySampleSeries && !info.supportsControllersDiscrimination || regexp.test(item.data[seriesIndex]))
                &&
                (!showControllersOnly || !info.supportsControllersDiscrimination || item.isController)){
            if(item.data.length > 0) {
                var newRow = tBody.insertRow(-1);
                for(var col=0; col < item.data.length; col++){
                    var cell = newRow.insertCell(-1);
                    cell.innerHTML = formatter ? formatter(col, item.data[col]) : item.data[col];
                }
            }
        }
    }

    // Add support of columns sort
    table.tablesorter({sortList : defaultSorts});
}

$(document).ready(function() {

    // Customize table sorter default options
    $.extend( $.tablesorter.defaults, {
        theme: 'blue',
        cssInfoBlock: "tablesorter-no-sort",
        widthFixed: true,
        widgets: ['zebra']
    });

    var data = {"OkPercent": 100.0, "KoPercent": 0.0};
    var dataset = [
        {
            "label" : "FAIL",
            "data" : data.KoPercent,
            "color" : "#FF6347"
        },
        {
            "label" : "PASS",
            "data" : data.OkPercent,
            "color" : "#9ACD32"
        }];
    $.plot($("#flot-requests-summary"), dataset, {
        series : {
            pie : {
                show : true,
                radius : 1,
                label : {
                    show : true,
                    radius : 3 / 4,
                    formatter : function(label, series) {
                        return '<div style="font-size:8pt;text-align:center;padding:2px;color:white;">'
                            + label
                            + '<br/>'
                            + Math.round10(series.percent, -2)
                            + '%</div>';
                    },
                    background : {
                        opacity : 0.5,
                        color : '#000'
                    }
                }
            }
        },
        legend : {
            show : true
        }
    });

    // Creates APDEX table
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.9353551912568306, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [0.9025, 500, 1500, "GET /departments [cached]"], "isController": false}, {"data": [0.9853333333333333, 500, 1500, "GET /analysis [JWT decode]"], "isController": false}, {"data": [1.0, 500, 1500, "GET Analysis Report"], "isController": false}, {"data": [0.905, 500, 1500, "GET /feedback [AOP toString]"], "isController": false}, {"data": [1.0, 500, 1500, "GET Prescriptions"], "isController": false}, {"data": [0.999, 500, 1500, "GET /appointments [4-table JOIN + parallelStream]"], "isController": false}, {"data": [0.8966666666666666, 500, 1500, "GET /prescriptions [AOP sequential DB]"], "isController": false}, {"data": [1.0, 500, 1500, "GET All Doctors"], "isController": false}, {"data": [1.0, 500, 1500, "GET All Appointments"], "isController": false}, {"data": [0.7866666666666666, 500, 1500, "GET /patients [AOP: toString full list]"], "isController": false}, {"data": [0.7086666666666667, 500, 1500, "POST Login [BCrypt+JWT]"], "isController": false}, {"data": [0.9025, 500, 1500, "GET /appointments [AOP + parallelStream]"], "isController": false}, {"data": [0.976, 500, 1500, "GET /admin/users [user hierarchy findAll()]"], "isController": false}, {"data": [0.998, 500, 1500, "GET /prescriptions [full findAll()]"], "isController": false}, {"data": [0.999, 500, 1500, "GET /feedback [full findAll()]"], "isController": false}, {"data": [0.99, 500, 1500, "GET /inventory [full findAll()]"], "isController": false}, {"data": [0.8626315789473684, 500, 1500, "POST Login"], "isController": false}, {"data": [0.8983333333333333, 500, 1500, "GET /doctors [AOP: toString full list]"], "isController": false}, {"data": [1.0, 500, 1500, "GET All Patients"], "isController": false}, {"data": [0.9713333333333334, 500, 1500, "GET /analysis/report [Redis KEYS O(N) + N+1 GETs]"], "isController": false}, {"data": [1.0, 500, 1500, "GET All Patients [G5]"], "isController": false}, {"data": [0.995, 500, 1500, "GET /patients [full table]"], "isController": false}, {"data": [0.8433333333333334, 500, 1500, "GET /analysis/report [Redis N+1]"], "isController": false}]}, function(index, item){
        switch(index){
            case 0:
                item = item.toFixed(3);
                break;
            case 1:
            case 2:
                item = formatDuration(item);
                break;
        }
        return item;
    }, [[0, 0]], 3);

    // Create statistics table
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 18300, 0, 0.0, 225.67224043715808, 12, 1801, 147.0, 546.9000000000015, 677.0, 950.9700000000048, 82.72308109574179, 928.3944309539147, 33.859486067319864], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["GET /departments [cached]", 600, 0, 0.0, 318.0816666666667, 15, 844, 297.0, 570.9, 631.8999999999999, 820.96, 19.592476489028215, 16.62681842672414, 8.041334002742946], "isController": false}, {"data": ["GET /analysis [JWT decode]", 750, 0, 0.0, 202.20933333333352, 15, 1179, 194.0, 345.9, 426.3499999999998, 1058.3500000000001, 27.25092653150207, 17.617298206889036, 16.598049685251798], "isController": false}, {"data": ["GET Analysis Report", 1000, 0, 0.0, 44.67599999999993, 27, 128, 43.0, 52.0, 57.0, 72.0, 8.347802858287698, 8.874439979819186, 3.4637919920570654], "isController": false}, {"data": ["GET /feedback [AOP toString]", 600, 0, 0.0, 328.7366666666665, 17, 1008, 322.0, 571.0, 663.0, 865.7800000000002, 19.58927813510072, 537.3469077508244, 7.9826308400535435], "isController": false}, {"data": ["GET Prescriptions", 1000, 0, 0.0, 25.23800000000001, 15, 66, 25.0, 30.0, 33.0, 43.0, 8.348220994106157, 359.60940239260015, 3.447660371934116], "isController": false}, {"data": ["GET /appointments [4-table JOIN + parallelStream]", 500, 0, 0.0, 143.6740000000003, 27, 576, 137.0, 235.90000000000003, 263.0, 390.7900000000002, 20.044097013429546, 103.97875325716576, 8.239220034074965], "isController": false}, {"data": ["GET /prescriptions [AOP sequential DB]", 600, 0, 0.0, 334.06500000000005, 22, 795, 336.5, 602.9, 673.4999999999993, 779.7900000000002, 19.58672020370189, 843.7209259621976, 8.077226765253158], "isController": false}, {"data": ["GET All Doctors", 1000, 0, 0.0, 19.260000000000023, 12, 68, 19.0, 22.0, 24.0, 32.97000000000003, 8.347802858287698, 43.622161225290505, 3.3985747822266927], "isController": false}, {"data": ["GET All Appointments", 1000, 0, 0.0, 21.506999999999977, 14, 81, 21.0, 25.0, 27.0, 37.0, 8.348081610845828, 43.30567335626273, 3.4394503857857215], "isController": false}, {"data": ["GET /patients [AOP: toString full list]", 600, 0, 0.0, 452.22000000000025, 24, 1276, 442.0, 795.8, 893.7999999999997, 1036.8300000000002, 19.573940560467165, 179.18566290412033, 7.97638077839037], "isController": false}, {"data": ["POST Login [BCrypt+JWT]", 750, 0, 0.0, 556.6680000000001, 140, 1360, 531.5, 828.0, 852.0, 915.47, 44.10467509556013, 27.737705821817116, 11.276841829608939], "isController": false}, {"data": ["GET /appointments [AOP + parallelStream]", 600, 0, 0.0, 332.8066666666665, 18, 986, 340.0, 584.5999999999999, 656.9499999999999, 799.95, 19.591197022138054, 101.62933455234115, 8.059940899888982], "isController": false}, {"data": ["GET /admin/users [user hierarchy findAll()]", 500, 0, 0.0, 293.65400000000017, 64, 748, 284.0, 445.90000000000003, 496.95, 561.98, 20.78397140125535, 1264.0267450741987, 8.523052022280417], "isController": false}, {"data": ["GET /prescriptions [full findAll()]", 500, 0, 0.0, 145.70600000000005, 32, 611, 137.0, 233.80000000000007, 276.69999999999993, 353.99, 20.33677702757667, 879.2265542381843, 8.379387659643701], "isController": false}, {"data": ["GET /feedback [full findAll()]", 500, 0, 0.0, 136.34799999999984, 29, 526, 129.0, 216.90000000000003, 249.79999999999995, 332.8800000000001, 20.604961674771285, 567.0879844741614, 8.389277950630513], "isController": false}, {"data": ["GET /inventory [full findAll()]", 500, 0, 0.0, 220.48800000000014, 33, 825, 216.5, 355.0, 402.84999999999997, 531.99, 20.784835384103758, 32.23273299800466, 8.482810941137346], "isController": false}, {"data": ["POST Login", 2850, 0, 0.0, 389.77017543859625, 114, 1801, 348.0, 737.9000000000001, 926.4499999999998, 1281.0, 13.965659179113254, 9.346569087748442, 5.480487585264024], "isController": false}, {"data": ["GET /doctors [AOP: toString full list]", 600, 0, 0.0, 324.6266666666669, 15, 1098, 324.0, 598.5999999999999, 657.7999999999997, 770.8300000000002, 19.571386632742932, 102.27495708565743, 7.956227370584206], "isController": false}, {"data": ["GET All Patients", 1000, 0, 0.0, 31.754, 21, 70, 31.0, 38.0, 41.0, 55.0, 8.345921765329372, 76.40104553534916, 3.405959261991003], "isController": false}, {"data": ["GET /analysis/report [Redis KEYS O(N) + N+1 GETs]", 750, 0, 0.0, 255.65733333333347, 32, 1238, 226.0, 408.9, 557.3999999999992, 1158.47, 27.204468787406146, 26.584398180474448, 11.256380298070297], "isController": false}, {"data": ["GET All Patients [G5]", 1000, 0, 0.0, 31.17700000000001, 20, 96, 30.0, 37.0, 40.94999999999993, 50.97000000000003, 8.3470363847316, 76.4112490922598, 3.406414136645159], "isController": false}, {"data": ["GET /patients [full table]", 500, 0, 0.0, 231.85000000000005, 33, 695, 231.0, 374.90000000000003, 419.0, 510.6700000000003, 20.86898451521349, 191.04087973204224, 8.496774437580868], "isController": false}, {"data": ["GET /analysis/report [Redis N+1]", 600, 0, 0.0, 407.1033333333334, 33, 1099, 411.5, 665.5999999999999, 737.8999999999999, 969.7700000000002, 19.595035924232526, 20.74630807070542, 8.118927580013063], "isController": false}]}, function(index, item){
        switch(index){
            // Errors pct
            case 3:
                item = item.toFixed(2) + '%';
                break;
            // Mean
            case 4:
            // Mean
            case 7:
            // Median
            case 8:
            // Percentile 1
            case 9:
            // Percentile 2
            case 10:
            // Percentile 3
            case 11:
            // Throughput
            case 12:
            // Kbytes/s
            case 13:
            // Sent Kbytes/s
                item = item.toFixed(2);
                break;
        }
        return item;
    }, [[0, 0]], 0, summaryTableHeader);

    // Create error table
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": []}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 18300, 0, "", "", "", "", "", "", "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
