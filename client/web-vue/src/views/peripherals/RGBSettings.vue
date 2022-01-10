<template>
    <div>
        <CModal title="Setari culoare"
                color="success"
                :show.sync="peripheral.show">
            <sketch-picker v-model="colors" :name="peripheral.data.uid"/>

            <template #footer>
                <CButton @click="peripheral.show = false" color="danger">Inchide</CButton>
            </template>
        </CModal>
    </div>
</template>
<script>
    import {Sketch} from 'vue-color'

    export default {
        name: 'RGBSettings',
        props: {
            peripheral: Object
        },
        components: {
            'sketch-picker': Sketch
        },
        data() {
            return {
                colors: {
                    hex: '#194d33',
                    hex8: '#194D33A8',
                    hsl: {h: 150, s: 0.5, l: 0.2, a: 1},
                    hsv: {h: 150, s: 0.66, v: 0.30, a: 1},
                    rgba: {r: 25, g: 77, b: 51, a: 1},
                    a: 1
                }
            }
        },
        methods: {
            loadInitial() {

            },
            openRGB: function () {
                this.peripheral.show = true;
                this.loadInitial();
            }
        }
    }
</script>