<script setup>
import {reactive, ref} from "vue";
import axios from "axios";

// NameServer数据
let nameServerIp = ref("http://localhost:700");
let isNameServerAvailable = ref(false);
let isNameServerRunning = ref(false);
let brokerTable = reactive({
  data: [
    {
      ip: "192.168.2.100",
      port: 7000,
      describe: "brokerA",
      topicInfo: {
        apple: [0, 1, 2, 3]
      }
    }
  ]
});

//NameServer方法
function testNameServer() {
  isNameServerAvailable.value = false;
  isNameServerRunning.value = false;
  axios.get(nameServerIp.value + '/testNameServer')
      .then(function (response) {
        if (response.data.status === 0) {
          isNameServerAvailable.value = true;
          if (response.data.info === 'running') {
            isNameServerRunning.value = true;
            getBrokersInfo();
          }
        }
      }).catch(() => {
  });
}

function initNameServer() {
  if (!isNameServerAvailable.value) {
    alert('请检查 NameServer 的 ip 地址。');
    return;
  }
  axios.get(nameServerIp.value + '/start')
      .then(function (response) {
        if (response.data.status === 0) {
          isNameServerRunning.value = true;
          getBrokersInfo();
        }
      })
      .catch(function (error) {
        console.log(error);
      });
}

function getBrokersInfo() {
  axios.get(nameServerIp.value + '/getAllBrokers')
      .then(function (response) {
        brokerTable.data = response.data.object;
      })
      .catch(function (error) {
        console.log(error);
      });
}

function shutdownNameServer() {
  axios.get(nameServerIp.value + '/shutdown')
      .then(function (response) {
        if (response.data.status === 0) {
          isNameServerRunning.value = false;
        }
      })
      .catch(function (error) {
        console.log(error);
      });
}

// Broker数据
let brokerIp = ref("http://localhost:700");
let isBrokerAvailable = ref(false);
let isBrokerRunning = ref(false);
let topicTable = reactive({
  data: null
});
let brokerTargetIp = ref("http://localhost:700");
let brokerTargetStatus = ref(0);

//Broker方法
function testBroker() {
  isBrokerAvailable.value = false;
  isBrokerRunning.value = false;
  axios.get(brokerIp.value + '/testBroker')
      .then(function (response) {
        if (response.data.status === 0) {
          isBrokerAvailable.value = true;
          if (response.data.info === 'running') {
            isBrokerRunning.value = true;
            getBrokerInfo();
          }
        }
      }).catch(() => {
  });
}

function initBroker() {
  if (!isBrokerAvailable.value) {
    alert('请检查 Broker 的 ip 地址。');
    return;
  } else if (brokerTargetStatus.value !== 2) {
    alert('NameServer 未注册');
    return;
  }
  axios.get(brokerIp.value + "/start")
      .then(function (response) {
        if (response.data.status === 0) {
          isBrokerRunning.value = true;
        }
      })
      .catch(function (error) {
        console.log(error);
      });
}

function shutdownBroker() {
  axios.get(brokerIp.value + '/shutdown')
      .then(function (response) {
        if (response.data.status === 0) {
          isBrokerRunning.value = false;
        }
      })
      .catch(function (error) {
        console.log(error);
      });

}

function getBrokerInfo() {
  axios.get(brokerIp.value + '/getBrokerRouting')
      .then(function (response) {
        // console.log(JSON.stringify(response.data));
        topicTable.data = response.data.topicInfo;
      })
      .catch(function (error) {
        console.log(error);
      });
}

function addTopic() {
  let data = JSON.stringify({
    "topic": document.getElementById('add-topic-input').value,
    "queueNum": document.getElementById('queue-num-input').value
  });

  let config = {
    method: 'post',
    url: brokerIp.value + '/addTopic',
    headers: {
      'Content-Type': 'application/json'
    },
    data: data
  };

  axios(config)
      .then(function (response) {
        if (response.data.status === 0) {
          getBrokerInfo();
          alert('添加topic成功');
        }
      })
      .catch(function (error) {
        console.log(error);
      });
}

function testTargetNameServer() {
  brokerTargetStatus.value = 0;
  axios.get(brokerTargetIp.value + '/testNameServer')
      .then(function (response) {
        if (response.data.status === 0) {
          brokerTargetStatus.value = 1;
          if (response.data.info === 'running') {
            registerNameServer();
          }
        }
      }).catch(() => {
  });
}

function registerNameServer() {
  let data = JSON.stringify({
    "nameServerUrl": brokerTargetIp.value
  });

  let config = {
    method: 'post',
    url: brokerIp.value + '/registerNameServer',
    headers: {
      'Content-Type': 'application/json'
    },
    data: data
  };

  axios(config)
      .then(function (response) {
        if (response.data.status === 0) {
          brokerTargetStatus.value = 2;
        }
      })
      .catch(function (error) {
        console.log(error);
      });
}
</script>

<template>
  <div>
    <div id="name-server" class="console">
      <h2 class="green">NameServer 控制台</h2>
      <div class="set-ip">
        <label for="input-name-ip">NameServer 的 IP 地址：</label>
        <input id="input-name-ip" type="text" v-model="nameServerIp" placeholder="url地址" @change="testNameServer"/>
        <span class="run-tip" v-if="isNameServerRunning">已启动</span>
        <span class="available-tip" v-else-if="isNameServerAvailable">已连接</span>
        <span class="down-tip" v-else>未连接</span>
      </div>
      <div id="register-info" v-if="isNameServerRunning">
        <table id="register-table">
          <caption>注册情况</caption>
          <tr>
            <th>ip地址</th>
            <th>端口号</th>
            <th>topic信息</th>
            <th>描述</th>
          </tr>
          <tr v-for="broker in brokerTable.data">
            <td v-text="broker.ip"></td>
            <td v-text="broker.port"></td>
            <td v-text="broker.topicInfo"></td>
            <td v-text="broker.describe"></td>
          </tr>
        </table>
      </div>
      <button class="run-btn" v-if="isNameServerRunning" @click="shutdownNameServer">关闭</button>
      <button class="down-btn" v-else @click="initNameServer">启动</button>
      <button v-if="isNameServerRunning" @click="getBrokersInfo">刷新NameServer信息</button>
    </div>

    <div id="broker" class="console">
      <h2 class="green">Broker 控制台</h2>
      <div class="set-ip">
        <label for="inputBrokerIp">Broker 的 IP 地址：</label>
        <input id="inputBrokerIp" type="text" v-model="brokerIp" placeholder="url地址" @change="testBroker"/>
        <span class="run-tip" v-if="isBrokerRunning">已启动</span>
        <span class="available-tip" v-else-if="isBrokerAvailable">已连接</span>
        <span class="down-tip" v-else>未连接</span>
      </div>
      <div class="broker-target-ip">
        <label for="inputTargetIp">注册 NameServer 的 IP 地址：</label>
        <input id="inputTargetIp" type="text" v-model="brokerTargetIp" placeholder="url地址"
               @change="testTargetNameServer"/>
        <span class="run-tip" v-if="brokerTargetStatus===2">已注册</span>
        <span class="available-tip" v-else-if="brokerTargetStatus===1">等待NameServer启动</span>
        <span class="down-tip" v-else>未连接</span>
      </div>
      <div id="topic-info" v-if="isBrokerAvailable">
        <table id="topic-table">
          <caption>topic 设置情况</caption>
          <tr>
            <th>topic名</th>
            <th>队列数量</th>
            <th>操作</th>
          </tr>
          <tr v-for="(value,key) in topicTable.data">
            <td v-text="key"></td>
            <td v-text="value.length"></td>
            <td></td>
          </tr>
        </table>
      </div>
      <div>
        <label for="add-topic-input">新增 topic：</label>
        <input id="add-topic-input" type="text" placeholder="topic名"/>
        <input id="queue-num-input" type="text" placeholder="消息队列数量"/>
        <button @click="addTopic">确认添加</button>
      </div>
      <button class="run-btn" v-if="isBrokerRunning" @click="shutdownBroker">关闭</button>
      <button class="down-btn" v-else @click="initBroker">启动</button>
      <button @click="getBrokerInfo" v-if="isBrokerAvailable">刷新broker信息</button>
    </div>
  </div>
</template>

<style>
.run-tip {
  color: #ff7371;
}

.available-tip {
  color: #9eff6a;
}

.down-tip {
  color: #9c9d99;
}

table {
  border: solid;
}

.console {
  border: solid;
  margin: 1rem;
  padding: 0.5rem;
}

#add-topic-input, #queue-num-input {
  width: 5rem;
}
</style>
