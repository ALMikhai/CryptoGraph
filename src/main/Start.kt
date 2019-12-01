package main

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jfoenix.controls.JFXCheckBox
import com.jfoenix.controls.JFXListView
import javafx.application.Application
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.chart.LineChart
import javafx.scene.chart.XYChart
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.stage.Stage
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneOffset


// TODO https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/Cell.html добавить поля в лист(выбор цвета, пометка того что крипта рисуется)
// TODO удаление с грфика.
class Coin() {
    var id = ""
    var symbol = ""
    var name = ""
    //var used : SimpleBooleanProperty = SimpleBooleanProperty(false)

    @Override
    override fun toString(): String {
        return name
    }
}

class Sender() {
    companion object {
        fun send(url : String) : String {
            return try {
                URL(url).openStream().bufferedReader().use{ it.readText() }
            } catch (e : Exception) {
                "not correct url"
            }
        }
    }
}

class PriceData {
    var prices : ArrayList<ArrayList<String>> = arrayListOf()
    var market_caps : ArrayList<ArrayList<String>> = arrayListOf()
    var total_volumes : ArrayList<ArrayList<String>> = arrayListOf()
}

class CryptoSeries() {
    companion object {
        fun generation(coin : Coin, from : String, to : String) : XYChart.Series<String, Number> {
            val json = Sender.send("https://api.coingecko.com/api/v3/coins/${coin.id}/market_chart/range?vs_currency=usd&from=$from&to=$to")
            val request = Gson().fromJson<PriceData>(
                json,
                PriceData::class.java
            )

            val series = XYChart.Series<String, Number>()
            series.name = coin.name
            request.prices.forEach { list: java.util.ArrayList<String> ->
                series.data.add(XYChart.Data<String, Number>(list[0], list[1].toDouble()))
            }
            return series
        }
    }
}

class ListRow(val coin: Coin) {
    val checkBox = JFXCheckBox()
    val label = Label()
    val colorPicker = ColorPicker()
    val hBox = HBox(checkBox, label, colorPicker)
    var series = XYChart.Series<String, Number>()
    var index = 0
    var checker = false
}

class MainStage() : Stage()  {
    private var loader : FXMLLoader = FXMLLoader()
    private val listCoins : ArrayList<Coin>

    private val dateFrom : DatePicker
    private val dateTo : DatePicker
    private val chart : LineChart<String, Number>
    private val coinsListView : JFXListView<Coin>

    private val coinsMap : MutableMap<Coin, Pair<Boolean, XYChart.Series<String, Number>>> = mutableMapOf()
    private val controlsMap : MutableMap<Coin, ListRow> = mutableMapOf()

    init {
        loader.location = javaClass.getResource("MainStage.fxml")
        val mainParent = loader.load<Parent>()
        scene = Scene(mainParent)

        listCoins = Gson().fromJson<ArrayList<Coin>>(
            Sender.send("https://api.coingecko.com/api/v3/coins/list"), object : TypeToken<List<Coin>>() {}.type
        )

        listCoins.forEach {
            controlsMap[it] = ListRow(it)
        }

        coinsListView = (mainParent.lookup("#CoinList") as JFXListView<Coin>)
        coinsListView.items.addAll(listCoins)

        chart = (mainParent.lookup("#MainChart") as LineChart<String, Number>)

        dateFrom = (mainParent.lookup("#DateFrom") as DatePicker)
        dateFrom.value = LocalDate.of(2018, Month.JANUARY, 1);
        dateTo = (mainParent.lookup("#DateTo") as DatePicker)
        dateTo.value = LocalDate.now()

        coinsListView.setCellFactory {
            object : ListCell<Coin>() {
                private lateinit var _coin : Coin

                override fun updateItem(coin: Coin?, empty: Boolean) {
                    super.updateItem(item, empty)

                    var row = ListRow(Coin())

                    if (coin != null) {
                        _coin = coin
                        row = controlsMap[_coin]!!
                    }

                    row.label.text = coin.toString()

                    if(row.checkBox.isSelected) {
                        row.colorPicker.opacity = 100.0
                        row.colorPicker.isDisable = false
                    }
                    else {
                        row.colorPicker.opacity = 0.0
                        row.colorPicker.isDisable = true
                    }

                    graphic = row.hBox

                    row.checkBox.onAction = EventHandler {
                        if (row.checkBox.isSelected) {
                            row.series = (CryptoSeries.generation(_coin,
                                    dateFrom.value.toEpochSecond(LocalTime.now(), ZoneOffset.UTC).toString(),
                                    dateTo.value.toEpochSecond(LocalTime.now(), ZoneOffset.UTC).toString()))
                            chart.data.add(row.series)
                            if(!row.checker) {
                                row.index = chart.data.indexOf(row.series)
                                row.checker = true
                            }
                        } else {
                            chart.data.remove(row.series)
                        }
                    }

                    row.colorPicker.onAction = EventHandler {
                        val color = row.colorPicker.value
                        chart.lookupAll(".default-color${row.index}.chart-series-line").forEachIndexed { i, node ->
                            node.style = "-fx-stroke: #${color.toString().substringAfter("x")};"
                        }
                    }
                }

                init {
                    contentDisplay = ContentDisplay.GRAPHIC_ONLY
                }
            }
        }
    }
}

open class AppStarter : Application() {
    @Throws(Exception::class)
    override fun start(primaryStage: Stage){
        val stage = Stage()
        stage.scene = MainStage().scene
        stage.show()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(AppStarter::class.java)
        }
    }
}