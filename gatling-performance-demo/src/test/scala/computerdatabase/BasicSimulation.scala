package computerdatabase

package computerdatabase

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class BasicSimulation extends Simulation {

  val baseUrl="http://127.0.0.1:8080/"

  val httpConf = http.baseUrls(baseUrl)

  // 定义模拟的请求，重复30次
  val helloRequest = repeat(30) {
       exec(http("reactive")
      .get("hello/100"))
      .pause(1 second, 2 seconds)
  }

  // 定义模拟的场景
  val scn = scenario("hello").exec(helloRequest)

  // 配置并发用户的数量在30秒内均匀提高至sim_users指定的数量
  setUp(scn.inject(rampUsers(3000).during(120 seconds)).protocols(httpConf))

}