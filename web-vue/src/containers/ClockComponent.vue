<template>
    <div id="clock">
        <p class="time">{{ time }}</p>
        <p class="date">{{ date }}</p>
    </div>
</template>
<script>
    export default {
        name: 'ClockComponent',
        data() {
            return {
                time: '',
                date: '',
                week: ['DU.', 'LU.', 'MA.', 'MI.', 'JO.', 'VI.', 'SA.'],
                timerID: setInterval(this.updateTime, 1000)
            }
        },
        created() {
            this.updateTime();
        },
        methods: {
            updateTime: function () {
                let cd = new Date();
                this.time = this.zeroPadding(cd.getHours(), 2) + ':' + this.zeroPadding(cd.getMinutes(), 2) + ':' + this.zeroPadding(cd.getSeconds(), 2);
                this.date = this.week[cd.getDay()] + ' ' + this.zeroPadding(cd.getDate(), 2) + '-' + this.zeroPadding(cd.getMonth() + 1, 2) + '-' + this.zeroPadding(cd.getFullYear(), 4);
            },
            zeroPadding: function (num, digit) {
                var zero = '';
                for (var i = 0; i < digit; i++) {
                    zero += '0';
                }
                return (zero + num).slice(-digit);
            }
        }
    }
</script>
<style scoped>
    #clock {
        font-family: 'Share Tech Mono', monospace;
        text-align: center;
        position: absolute;
        right: 170px;
        top: 23%;
        transform: translate(-50%, -50%);
        color: #6a7a7f;
        text-shadow: 0 0 20px rgb(7, 82, 108), 0 0 20px rgba(10, 175, 230, 0);
    }

    #clock .time {
        letter-spacing: 0.05em;
        font-size: 15px;
        padding: 2px 0;
    }

    #clock .date {
        letter-spacing: 0.1em;
        font-size: 10px;
    }

    #clock .text {
        letter-spacing: 0.5em;
        font-size: 12px;
        padding: 2px 0 0;
    }

    #clock p {
        margin: 0;
        padding: 0;
    }

</style>